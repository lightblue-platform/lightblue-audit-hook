package com.redhat.lightblue.hook.audit.model;

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

    @Override
    public String toString() {
        StringBuilder buff = new StringBuilder("{");

        // both fields are required, will simply add them
        String escapeJsonField = StringEscapeUtils.escapeJson(getFieldText().toString());
        String escapeJsonValue = StringEscapeUtils.escapeJson(getValueText().toString());
        buff.append("\"fieldText\":\"").append(escapeJsonField).append("\",");
        buff.append("\"valueText\":\"").append(escapeJsonValue).append("\"");

        buff.append("}");

        return buff.toString();
    }
}
