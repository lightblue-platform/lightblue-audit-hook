package com.redhat.lightblue.hook.audit.model;

/**
 *
 * @author nmalik
 */
public class AuditIdentity {
    public String fieldText;
    public String valueText;

    public String toJson() {
        StringBuilder buff = new StringBuilder("{");

        // both fields are required, will simply add them
        buff.append("\"fieldText\":\"").append(fieldText).append("\",");
        buff.append("\"valueText\":\"").append(valueText).append("\"");

        buff.append("}");

        return buff.toString();
    }
}
