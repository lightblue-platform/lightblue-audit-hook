package com.redhat.lightblue.hook.audit;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.lightblue.ClientIdentification;
import com.redhat.lightblue.DataError;
import com.redhat.lightblue.Response;
import com.redhat.lightblue.config.LightblueFactory;
import com.redhat.lightblue.config.LightblueFactoryAware;
import com.redhat.lightblue.crud.CRUDOperation;
import com.redhat.lightblue.crud.InsertionRequest;
import com.redhat.lightblue.hook.audit.model.Audit;
import com.redhat.lightblue.hook.audit.model.AuditData;
import com.redhat.lightblue.hook.audit.model.AuditIdentity;
import com.redhat.lightblue.hooks.CRUDHook;
import com.redhat.lightblue.hooks.HookDoc;
import com.redhat.lightblue.metadata.EntityMetadata;
import com.redhat.lightblue.metadata.Field;
import com.redhat.lightblue.metadata.HookConfiguration;
import com.redhat.lightblue.metadata.types.DateType;
import com.redhat.lightblue.util.Error;
import com.redhat.lightblue.util.JsonNodeCursor;
import com.redhat.lightblue.util.Path;

/**
 * Audit hook implementation that writes audit data to a lightblue entity.
 *
 * @author nmalik
 */
public class AuditHook implements CRUDHook, LightblueFactoryAware {
    private final Logger LOGGER = LoggerFactory.getLogger(AuditHook.class);

    public static final String HOOK_NAME = "auditHook";

    public static final String ERR_MISSING_ID = "audit-hook:MissingID";

    private LightblueFactory lightblueFactory;

    @Override
    public void setLightblueFactory(LightblueFactory lightblueFactory) {
        this.lightblueFactory = lightblueFactory;
    }

    @Override
    public String getName() {
        return HOOK_NAME;
    }

    protected Audit findAuditFor(EntityMetadata md, HookConfiguration cfg, HookDoc processedDocument) {
        Audit audit = new Audit();
        JsonNodeCursor preCursor = null;
        JsonNodeCursor postCursor = null;

        // find if each field changed

        //  for CRUDOperation.INSERT && CRUDOperation.FIND,  preCursor is null
        if (processedDocument.getPreDoc() != null) {
            preCursor = processedDocument.getPreDoc().cursor();
        }
        // for  CRUDOperation.DELETE, postCursor is null
        if (processedDocument.getPostDoc() != null) {
            postCursor = processedDocument.getPostDoc().cursor();
        }

        if (preCursor != null) {
            while (preCursor.next()) {
                Path path = preCursor.getCurrentPath();
                JsonNode node = preCursor.getCurrentNode();

                if (node.isValueNode()) {
                    // non-container node, check if it changed
                    String preValue = node.asText();
                    String postValue = null;
                    if (processedDocument.getPostDoc() != null) {
                        // if operation is delete, post value doesn't exist.
                        JsonNode postNode = processedDocument.getPostDoc().get(path);
                        postValue = processedDocument.getCRUDOperation() == CRUDOperation.DELETE || postNode == null ? null : postNode.asText();
                    }

                    if ((preValue != null && !preValue.equals(postValue))
                            || (preValue == null && postValue != null)) {
                        // something changed! audit it..
                        AuditData ad = new AuditData();
                        ad.setFieldText(path);
                        ad.setOldValue(preValue);
                        ad.setNewValue(postValue);
                        audit.addData(path, ad);
                    }
                }
                // else continue processing
            }
        }

        if (postCursor != null) {
            while (postCursor.next()) {
                Path path = postCursor.getCurrentPath();
                JsonNode node = postCursor.getCurrentNode();

                // shortcut, don't check if we have an audit for the path already
                if (!audit.getData().containsKey(path) && node.isValueNode()) {

                    String preValue = null;
                    if (processedDocument.getPreDoc() != null) {
                        // non-container node, check if it changed
                        JsonNode preNode = processedDocument.getPreDoc().get(path);
                        preValue = preNode == null ? null : preNode.asText();
                    }
                    String postValue = node.asText();

                    if ((preValue != null && !preValue.equals(postValue))
                            || (preValue == null && postValue != null)) {
                        // something changed! audit it..
                        AuditData ad = new AuditData();
                        ad.setFieldText(path);
                        ad.setOldValue(preValue);
                        ad.setNewValue(postValue);
                        audit.addData(path, ad);
                    }
                }
                // else continue processing
            }
        }

        // if there is nothing to audit, return null (meaning nothing to audit)
        if (audit.getData().isEmpty()) {
            return null;
        }

        // simple optimization, set other fields on audit only if there is data
        // set top level things like last update who/when
        audit.setEntityName(processedDocument.getEntityMetadata().getName());
        audit.setVersionText(processedDocument.getEntityMetadata().getVersion().getValue());
        audit.setLastUpdateDate(DateType.getDateFormat().format(processedDocument.getWhen()));
        audit.setLastUpdatedBy(processedDocument.getWho());
        audit.setCRUDOperation(processedDocument.getCRUDOperation().toString());

        // attempt to get set of fields that identify the document from:
        //  1) pre doc
        //  2) post doc
        // if not found, fail.. need identity to audit!
        for (Field f : processedDocument.getEntityMetadata().getEntitySchema().getIdentityFields()) {
            Path p = f.getFullPath();
            JsonNode node = null;
            // pre doc?
            if (processedDocument.getPreDoc() != null) {
                node = processedDocument.getPreDoc().get(p);
            }

            if (node == null && processedDocument.getPostDoc() != null) {
                // post doc?
                node = processedDocument.getPostDoc().get(p);
            }

            if (node == null) {
                // unable to find a path for identity, fail
                throw Error.get(ERR_MISSING_ID, "path:" + p.toString());
            }

            AuditIdentity identity = new AuditIdentity();
            identity.setFieldText(p.toString());
            identity.setValueText(node.asText());
            audit.addIdentity(identity);
        }

        return audit;
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
                Audit audit = findAuditFor(md, cfg, hd);

                // if there's nothing to audit, stop
                if (audit == null || audit.getData().isEmpty()) {
                    return;
                }

                /*
                 Structure is noted here as one builder is created for all of this
                 - common bits: http://docs.lightblue.io/language_specification/data.html#common-request
                 --- specifically: entity, entityVersion
                 - insert bits: http://docs.lightblue.io/language_specification/data.html#insert
                 --- specifically: audit toString
                 */
                // common bits: (note, includes starting { for first data element and _id field name and first paren for value)
                // note that entity (name) is for audit, not the audited entity
                ObjectNode jsonNode = new ObjectNode(JsonNodeFactory.instance);
                jsonNode.put("entity", "audit");
                ArrayNode data = jsonNode.putArray("data");
                data.add(audit.toJSON());

                // All data prepared, do the insert!
                try {
                    // create insert request
                    InsertionRequest ireq = InsertionRequest.fromJson(jsonNode);
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
                    Response r = lightblueFactory.getMediator().insert(ireq);
                    if (!r.getErrors().isEmpty()) {
                        // there are errors.  there is nowhere to return errors so just log them for now
                        for (Error e : r.getErrors()) {
                            LOGGER.error(e.toString());
                        }
                    }
                    else if (!r.getDataErrors().isEmpty()) {
                        //TODO Better Handle Exception
                        for (DataError e : r.getDataErrors()) {
                            LOGGER.error(e.toString());
                        }
                    }
                } catch (Error e) {
                    LOGGER.error("insert error: {}", e);
                } catch (Exception e) {
                    LOGGER.error("insert exception: {}", e);
                }
            }
        } catch (Error e) {
            LOGGER.error("audit error: {}", e);
            throw e;
        } catch (Exception e) {
            LOGGER.error("audit exception: {}", e);
            throw e;
        } finally {
            Error.pop();
            Error.pop();
        }
    }

}
