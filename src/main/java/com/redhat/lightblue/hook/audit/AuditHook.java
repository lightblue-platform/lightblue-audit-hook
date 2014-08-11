package com.redhat.lightblue.hook.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.lightblue.config.LightblueFactory;
import com.redhat.lightblue.crud.Operation;
import com.redhat.lightblue.hooks.CRUDHook;
import com.redhat.lightblue.hooks.HookDoc;
import com.redhat.lightblue.metadata.EntityMetadata;
import com.redhat.lightblue.metadata.Field;
import com.redhat.lightblue.metadata.HookConfiguration;
import com.redhat.lightblue.metadata.types.DateType;
import java.util.List;
import com.redhat.lightblue.util.Error;
import com.redhat.lightblue.util.JsonNodeCursor;
import com.redhat.lightblue.util.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Audit hook implementation that writes audit data to a lightblue entity.
 *
 * @author nmalik
 */
public class AuditHook implements CRUDHook {
    public static final String HOOK_NAME = "auditHook";

    public static final String ERR_MISSING_ID = "audit-hook:MissingID";

    private static final class AuditData {
        Path path;
        String pre;
        String post;
    }

    @Override
    public String getName() {
        return HOOK_NAME;
    }

    @Override
    public void processHook(EntityMetadata md, HookConfiguration cfg, List<HookDoc> processedDocuments) {
        if (!(cfg instanceof AuditHookConfiguration)) {
            // fail
        }
        AuditHookConfiguration auditConfig = (AuditHookConfiguration) cfg;

        LightblueFactory factory = LightblueFactory.getInstance();
        try {
            Error.push(auditConfig.getEntityName());
            Error.push(auditConfig.getVersion());

            // for each processed document
            for (HookDoc hd : processedDocuments) {
                // find if each field changed
                JsonNodeCursor preCursor = hd.getPreDoc().cursor();

                // record to hold changes
                Map<Path, AuditData> audits = new HashMap<>();

                while (preCursor.next()) {
                    Path path = preCursor.getCurrentPath();
                    JsonNode node = preCursor.getCurrentNode();

                    if (node.isValueNode()) {
                        // non-container node, check if it changed
                        String preValue = node.asText();
                        // if operation is delete, post value doesn't exist.
                        String postValue = hd.getOperation() == Operation.DELETE ? null : hd.getPostDoc().get(path).asText();

                        if ((preValue != null && preValue.equals(postValue))
                                || (preValue == null && postValue != null)) {
                            // something changed! audit it..
                            AuditData ad = new AuditData();
                            ad.path = path;
                            ad.pre = preValue;
                            ad.post = postValue;
                            audits.put(path, ad);
                        }
                    }
                    // else continue processing
                }

                List<JsonNode> identifyingNodes = new ArrayList<>();

                if (!audits.isEmpty()) {
                    String when = DateType.toString(hd.getWhen());

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

                    // build key for identity
                    StringBuilder identityString = new StringBuilder();
                    for (JsonNode node : identifyingNodes) {
                        identityString.append(node.asText()).append("|");
                    }
                    // NOTE: for simplicity I'm going to leave the trailing pipe (|) to simplify this

                    // see metadata/audit.json for structure
                    StringBuilder buff = new StringBuilder(String.format("{\"_id\" : \"%s|%s\",\"audits\":[", hd.getEntityName(), identityString.toString()));

                    // audit those that did change
                    for (Entry<Path, AuditData> e : audits.entrySet()) {
                        Path p = e.getKey();
                        AuditData ad = e.getValue();

                        buff.append(String.format("{\"field\":\"%s\",\"value\":\"%s\",\"when\":\"%s\"},", ad.path.toString(), ad.post, when));
                    }

                    // trim last char, it's going to be tailing ','
                    buff.replace(buff.length() - 1, buff.length(), "");

                    // and close it
                    buff.append("]}");
                }
            }
        } finally {
            Error.pop();
            Error.pop();
        }
    }
}
