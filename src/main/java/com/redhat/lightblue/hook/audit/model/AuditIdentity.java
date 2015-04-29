package com.redhat.lightblue.hook.audit.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringEscapeUtils;

/**
 * @author nmalik
 */
public class AuditIdentity {
    private String fieldText;
    private String valueText;

    public String getFieldText() {
        return fieldText;
    }

    public void setFieldText(String fieldText) {
        this.fieldText = fieldText;
    }

    public String getValueText() {
        return valueText;
    }

    public void setValueText(String valueText) {
        this.valueText = valueText;
    }

    public void toJSON(ObjectNode objectNode) {
        objectNode.put("fieldText",getFieldText());
        objectNode.put("valueText",getValueText());
    }
}
