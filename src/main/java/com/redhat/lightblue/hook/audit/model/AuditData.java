package com.redhat.lightblue.hook.audit.model;

import com.redhat.lightblue.util.Path;

/**
 *
 * @author nmalik
 */
public class AuditData {
    public Path fieldText;
    public String oldValue;
    public String newValue;

    public String toJson() {
        StringBuilder buff = new StringBuilder("{");

        toJson(buff, "fieldText", fieldText);
        toJson(buff, "oldValue", oldValue);
        toJson(buff, "newValue", newValue);

        // remove trailing comma
        buff.deleteCharAt(buff.length() - 1);

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
