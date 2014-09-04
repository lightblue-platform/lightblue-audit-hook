package com.redhat.lightblue.hook.audit.model;

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
        buff.append("\"fieldText\":\"").append(getFieldText()).append("\",");
        buff.append("\"valueText\":\"").append(getValueText()).append("\"");

        buff.append("}");

        return buff.toString();
    }
}
