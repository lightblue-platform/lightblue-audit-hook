package com.redhat.lightblue.hook.audit.model;

import com.redhat.lightblue.util.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author nmalik
 */
public class Audit {
    public String entityName;
    public String versionText;
    public String lastUpdateDate;
    public String lastUpdatedBy;
    public List<AuditIdentity> identity = new ArrayList<>();
    public Map<Path, AuditData> data = new HashMap<>();

    public String toJson() {
        StringBuilder buff = new StringBuilder("{");

        toJson(buff, "entityName", entityName);
        toJson(buff, "versionText", versionText);
        toJson(buff, "lastUpdateDate", lastUpdateDate);
        toJson(buff, "lastUpdatedBy", lastUpdatedBy);

        // remove trailing comma
        buff.deleteCharAt(buff.length() - 1);

        if (identity != null && !identity.isEmpty()) {
            buff.append(",\"identity\":[");
            for (AuditIdentity i : identity) {
                buff.append(i.toJson()).append(",");
            }
            // remove trailing comma
            buff.deleteCharAt(buff.length() - 1);
            buff.append("]");
        }

        if (data != null && !data.isEmpty()) {
            buff.append(",\"audits\":[");
            for (AuditData d : data.values()) {
                buff.append(d.toJson()).append(",");
            }
            // remove trailing comma
            buff.deleteCharAt(buff.length() - 1);
            buff.append("]");
        }

        buff.append("}");

        return buff.toString();
    }

    /**
     * If name or value is empty does nothing, else adds to the buffer. Note,
     * always adds trailing comma! Strip it off if you don't want it.
     *
     * @param buff builder to add the text to
     * @param name name of field
     * @param value value of field
     */
    private void toJson(StringBuilder buff, String name, Object value) {
        if (name != null && !name.isEmpty() && value != null && !value.toString().isEmpty()) {
            buff.append(String.format("\"%s\":\"%s\",", name, value.toString()));
        }
    }

}
