package com.redhat.lightblue.hook.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.lightblue.config.LightblueFactory;
import com.redhat.lightblue.crud.Operation;
import com.redhat.lightblue.hooks.CRUDHook;
import com.redhat.lightblue.hooks.HookDoc;
import com.redhat.lightblue.metadata.EntityMetadata;
import com.redhat.lightblue.metadata.HookConfiguration;
import com.redhat.lightblue.metadata.MetadataConstants;
import com.redhat.lightblue.metadata.types.DateType;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import com.redhat.lightblue.util.Error;
import com.redhat.lightblue.util.JsonNodeCursor;
import com.redhat.lightblue.util.Path;
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

            // get the metadata
            EntityMetadata metadata = factory.getMetadata().getEntityMetadata(auditConfig.getEntityName(), auditConfig.getVersion());

            // for each processed document
            for (HookDoc hd : processedDocuments) {

                // fail if id path is missing
                if (hd.getIdentifyingPath() == null) {
                    throw Error.get(ERR_MISSING_ID, "path:null");
                }

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

                if (!audits.isEmpty()) {
                    // attempt to get id from pre doc
                    JsonNode id = hd.getPreDoc().get(hd.getIdentifyingPath());
                    String when = DateType.toString(hd.getWhen());

                    if (id == null && hd.getPostDoc() != null) {
                        // didn't find id in pre, try to find in post
                        id = hd.getPostDoc().get(hd.getIdentifyingPath());
                    }

                    if (id == null) {
                        throw Error.get(ERR_MISSING_ID, "path:" + hd.getIdentifyingPath().toString());
                    }

                    // see metadata/audit.json for structure
                    StringBuilder buff = new StringBuilder(String.format("{\"_id\" : \"%s|%s\",\"audits\":[", hd.getEntityName(), id.asText()));

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

        } catch (IOException | ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException ex) {
            throw Error.get(MetadataConstants.ERR_ILL_FORMED_METADATA);
        } finally {
            Error.pop();
            Error.pop();
        }
    }
}
