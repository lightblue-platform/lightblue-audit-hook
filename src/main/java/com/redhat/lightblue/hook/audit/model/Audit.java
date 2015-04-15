package com.redhat.lightblue.hook.audit.model;

import com.redhat.lightblue.util.Path;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author nmalik
 */
public class Audit {
    private String entityName;
    private String versionText;
    private String lastUpdateDate;
    private String lastUpdatedBy;
    private final List<AuditIdentity> identity = new ArrayList<>();
    private final Map<Path, AuditData> data = new HashMap<>();

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getVersionText() {
        return versionText;
    }

    public void setVersionText(String versionText) {
        this.versionText = versionText;
    }

    public String getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(String lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    public String getLastUpdatedBy() {
        return lastUpdatedBy;
    }

    public void setLastUpdatedBy(String lastUpdatedBy) {
        this.lastUpdatedBy = lastUpdatedBy;
    }

    /**
     * @param i the audited object's identity field (object can have many)
     */
    public void addIdentity(AuditIdentity i) {
        identity.add(i);
    }

    /**
     * WARNING: this is not a copy.
     */
    public List<AuditIdentity> getIdentity() {
        return identity;
    }

    /**
     * @param p the path to the data being audited
     * @param d the auditable data
     */
    public void addData(Path p, AuditData d) {
        data.put(p, d);
    }

    /**
     * WARNING: this is not a copy.
     */
    public Map<Path, AuditData> getData() {
        return data;
    }


    @Override
    public String toString() {
        StringBuilder buff = new StringBuilder("{");

        toString(buff, "entityName", getEntityName());
        toString(buff, "versionText", getVersionText());
        toString(buff, "lastUpdateDate", getLastUpdateDate());
        toString(buff, "lastUpdatedBy", getLastUpdatedBy());

        // remove trailing comma
        buff.deleteCharAt(buff.length() - 1);

        if (identity != null && !identity.isEmpty()) {
            buff.append(",\"identity\":[");
            for (AuditIdentity i : identity) {
                buff.append(i.toString()).append(",");
            }
            // remove trailing comma
            buff.deleteCharAt(buff.length() - 1);
            buff.append("]");
        }

        if (data != null && !data.isEmpty()) {
            buff.append(",\"audits\":[");
            for (AuditData d : data.values()) {
                buff.append(d.toString()).append(",");
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
     * @param buff  builder to add the text to
     * @param name  name of field
     * @param value value of field
     */
    private void toString(StringBuilder buff, String name, Object value) {
        if (name != null && !name.isEmpty() && value != null && !value.toString().isEmpty()) {
            buff.append(String.format("\"%s\":\"%s\",", name, value.toString()));
        }
    }
}
