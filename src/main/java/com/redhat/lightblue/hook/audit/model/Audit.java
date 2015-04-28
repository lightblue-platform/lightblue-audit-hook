package com.redhat.lightblue.hook.audit.model;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.lightblue.util.Path;
import org.apache.commons.lang3.StringEscapeUtils;

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



    public ObjectNode toJSON() {
        ObjectNode jsonNode = new ObjectNode(JsonNodeFactory.instance);
        toJSON(jsonNode, "entityName", getEntityName());
        toJSON(jsonNode, "versionText", getVersionText());
        toJSON(jsonNode, "lastUpdateDate", getLastUpdateDate());
        toJSON(jsonNode, "lastUpdatedBy", getLastUpdatedBy());

        if (identity != null && !identity.isEmpty()) {
            ArrayNode arrayJsonNode = jsonNode.putArray("identity");
            for (AuditIdentity i : identity) {
                ObjectNode identityNode = new ObjectNode(JsonNodeFactory.instance);
                i.toJSON(identityNode);
                arrayJsonNode.add(identityNode);
            }
        }

        if (data != null && !data.isEmpty()) {
            ArrayNode arrayJsonNode = jsonNode.putArray("audits");
            for (AuditData d : data.values()) {
                ObjectNode dataNode = new ObjectNode(JsonNodeFactory.instance);
                d.toJSON(dataNode);
                arrayJsonNode.add(dataNode);
            }
        }

        return jsonNode;
    }

    private void toJSON(ObjectNode jsonNode, String name, String value) {
        if (name != null && !name.isEmpty() && value != null && !value.isEmpty()) {
            jsonNode.put(name, value);
        }
    }
}
