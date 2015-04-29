package com.redhat.lightblue.hook.audit.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.lightblue.util.Path;
import org.apache.commons.lang3.StringEscapeUtils;

/**
 * @author nmalik
 */
public class AuditData {
    private Path fieldText;
    private String oldValue;
    private String newValue;

    public Path getFieldText() {
        return fieldText;
    }

    public void setFieldText(Path fieldText) {
        this.fieldText = fieldText;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public void toJSON(ObjectNode objectNode) {
        toJSON(objectNode, "fieldText", getFieldText());
        toJSON(objectNode, "oldValue", getOldValue());
        toJSON(objectNode, "newValue", getNewValue());
    }

    private void toJSON(ObjectNode jsonNode, String name, Object value) {
        if (name != null && !name.isEmpty() && value != null && !value.toString().isEmpty()) {
            jsonNode.put(name, value.toString());
        }
    }
}
