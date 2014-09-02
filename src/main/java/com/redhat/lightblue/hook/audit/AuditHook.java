package com.redhat.lightblue.hook.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.lightblue.ClientIdentification;
import com.redhat.lightblue.Response;
import com.redhat.lightblue.crud.InsertionRequest;
import com.redhat.lightblue.crud.Operation;
import com.redhat.lightblue.hooks.CRUDHook;
import com.redhat.lightblue.hooks.HookDoc;
import com.redhat.lightblue.metadata.EntityMetadata;
import com.redhat.lightblue.metadata.Field;
import com.redhat.lightblue.metadata.HookConfiguration;
import com.redhat.lightblue.metadata.types.DateType;
import com.redhat.lightblue.rest.RestConfiguration;
import java.util.List;
import com.redhat.lightblue.util.Error;
import com.redhat.lightblue.util.JsonNodeCursor;
import com.redhat.lightblue.util.JsonUtils;
import com.redhat.lightblue.util.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Audit hook implementation that writes audit data to a lightblue entity.
 *
 * @author nmalik
 */
public class AuditHook implements CRUDHook {
    private final Logger LOGGER = LoggerFactory.getLogger(AuditHook.class);

    public static final String HOOK_NAME = "auditHook";

    public static final String ERR_MISSING_ID = "audit-hook:MissingID";

    /**
     * Modeled off the metadata for audit.
     */
    protected static final class AuditData {
        Path fieldText;
        String oldValue;
        String newValue;

        public String toJson() {
            StringBuilder buff = new StringBuilder("{");

            toJson(buff, "fieldText", fieldText.toString());
            toJson(buff, "oldValue", oldValue);
            toJson(buff, "newValue", newValue);

            // remove trailing comma
            buff.deleteCharAt(buff.length() - 1);

            buff.append("}");

            return buff.toString();
        }

        /**
         * If name or value is empty does nothing, else adds to the buffer.
         * Note, always adds trailing comma! Strip it off if you don't want it.
         *
         * @param buff builder to add the text to
         * @param name name of field
         * @param value value of field
         */
        private void toJson(StringBuilder buff, String name, String value) {
            if (name != null && !name.isEmpty() && value != null && !value.isEmpty()) {
                buff.append(String.format("\"%s\":\"%s\",", name, value));
            }
        }
    }

    @Override
    public String getName() {
        return HOOK_NAME;
    }

    protected Map<Path, AuditData> findAuditsFor(EntityMetadata md, HookConfiguration cfg, HookDoc processedDocument) {
        // find if each field changed
        JsonNodeCursor preCursor = processedDocument.getPreDoc().cursor();
        JsonNodeCursor postCursor = processedDocument.getPostDoc().cursor();

        // record to hold changes
        Map<Path, AuditData> audits = new HashMap<>();

        while (preCursor.next()) {
            Path path = preCursor.getCurrentPath();
            JsonNode node = preCursor.getCurrentNode();

            if (node.isValueNode()) {
                // non-container node, check if it changed
                String preValue = node.asText();
                // if operation is delete, post value doesn't exist.
                JsonNode postNode = processedDocument.getPostDoc().get(path);
                String postValue = processedDocument.getOperation() == Operation.DELETE || postNode == null ? null : postNode.asText();

                if ((preValue != null && !preValue.equals(postValue))
                        || (preValue == null && postValue != null)) {
                    // something changed! audit it..
                    AuditData ad = new AuditData();
                    ad.fieldText = path;
                    ad.oldValue = preValue;
                    ad.newValue = postValue;
                    audits.put(path, ad);
                }
            }
            // else continue processing
        }

        while (postCursor.next()) {
            Path path = postCursor.getCurrentPath();
            JsonNode node = postCursor.getCurrentNode();

            // shortcut, don't check if we have an audit for the path already
            if (!audits.containsKey(path) && node.isValueNode()) {
                // non-container node, check if it changed
                JsonNode preNode = processedDocument.getPreDoc().get(path);
                String preValue = preNode == null ? null : preNode.asText();
                String postValue = node.asText();

                if ((preValue != null && !preValue.equals(postValue))
                        || (preValue == null && postValue != null)) {
                    // something changed! audit it..
                    AuditData ad = new AuditData();
                    ad.fieldText = path;
                    ad.oldValue = preValue;
                    ad.newValue = postValue;
                    audits.put(path, ad);
                }
            }
            // else continue processing
        }

        return audits;
    }

    @Override
    public void processHook(EntityMetadata md, HookConfiguration cfg, List<HookDoc> processedDocuments) {
        if (!(cfg instanceof AuditHookConfiguration)) {
            // fail
        }
        AuditHookConfiguration auditConfig = (AuditHookConfiguration) cfg;

        try {
            Error.push(auditConfig.getEntityName());
            Error.push(auditConfig.getVersion());

            // for each processed document
            for (HookDoc hd : processedDocuments) {
                Map<Path, AuditData> audits = findAuditsFor(md, cfg, hd);

                // if there's nothing to audit, stop
                if (audits.isEmpty()) {
                    return;
                }

                List<JsonNode> identifyingNodes = new ArrayList<>();

                if (!audits.isEmpty()) {
                    String lastUpdateDate = DateType.getDateFormat().format(hd.getWhen());

                    // attempt to get set of fields that identify the document from:
                    //  1) pre doc
                    //  2) post doc
                    // if not found, fail.. need identity to audit!
                    for (Field f : hd.getEntityMetadata().getEntitySchema().getIdentityFields()) {
                        Path p = f.getFullPath();

                        // pre doc?
                        JsonNode node = hd.getPreDoc().get(p);

                        if (node == null && hd.getPostDoc() != null) {
                            // post doc?
                            node = hd.getPostDoc().get(p);
                        }

                        if (node == null) {
                            // unable to find a path for identity, fail
                            throw Error.get(ERR_MISSING_ID, "path:" + p.toString());
                        }

                        identifyingNodes.add(node);
                    }

                    /*
                    Structure is noted here as one builder is created for all of this
                    - common bits: http://docs.lightblue.io/language_specification/data.html#common-request
                    --- specifically: entity, entityVersion
                    - insert bits: http://docs.lightblue.io/language_specification/data.html#insert
                    --- specifically: data
                    - as part of data, things to note:
                    --- _id is concatenation of all identity fields
                    */
                    
                    StringBuilder buff = new StringBuilder();
                    // common bits: (note, includes starting { for first data element and _id field name and first paren for value)
                    buff.append(String.format("{\"entity\":\"%s\",\"entityVersion\":\"%s\",\"data\":[{\"_id\":\"%s|", 
                            hd.getEntityMetadata().getName(),
                            hd.getEntityMetadata().getVersion().getValue(),
                            hd.getEntityMetadata().getName()));
                    
                    // insert bits: _id (concatenation of identity fields starting with entity name)
                    for (JsonNode node : identifyingNodes) {
                        buff.append(node.asText()).append("|");
                    }
                    // NOTE: for simplicity I'm going to leave the trailing pipe (|) to simplify this
                    // insert bits: close _id field and add header data for audit
                    // see metadata/audit.json for full data structure
                    buff.append(String.format("\",\"entityName\":\"%s\",\"versionText\":\"%s\",\"lastUpdateDate\":\"%s\",\"audits\":[",
                            hd.getEntityMetadata().getName(),
                            hd.getEntityMetadata().getVersion().getValue(),
                            lastUpdateDate));

                    // add each thing to be audited (includes a trailing comma)
                    for (Entry<Path, AuditData> e : audits.entrySet()) {
                        AuditData ad = e.getValue();

                        // append the individual audit and always a comma.  
                        buff.append(ad.toJson()).append(",");
                    }

                    // trim last char, it is the tailing ','
                    buff.deleteCharAt(buff.length() - 1);

                    // and close the data and insert request
                    buff.append("]}]}");

                    // All data prepared, do the insert!
                    try {
                        // create insert request
                        InsertionRequest ireq = InsertionRequest.fromJson((ObjectNode) JsonUtils.json(buff.toString()));
                        // add client identifier bits
                        ireq.setClientId(new ClientIdentification() {
                            @Override
                            public boolean isUserInRole(String role) {
                                // audit hook is only doing insert and must always be allowed to do insert.
                                return true;
                            }

                            @Override
                            public String getPrincipal() {
                                return HOOK_NAME;
                            }
                        });
                        // issue insert against crud mediator
                        Response r = RestConfiguration.getFactory().getMediator().insert(ireq);
                        if (!r.getErrors().isEmpty()) {
                            // there are errors.  there is nowhere to return errors so just log them for now
                            for (Error e: r.getErrors()) {
                                LOGGER.error(e.toString());
                            }
                        }
                    } catch (Error e) {
                        LOGGER.error("insert failure: {}", e);
                    } catch (Exception e) {
                        LOGGER.error("insert failure: {}", e);
                    }
                }
            }
        } finally {
            Error.pop();
            Error.pop();
        }
    }
}
