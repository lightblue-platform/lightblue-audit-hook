package com.redhat.lightblue.hook.audit.model;

import com.redhat.lightblue.util.Path;

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

    @Override
    public String toString() {
        StringBuilder buff = new StringBuilder("{");

        toString(buff, "fieldText", getFieldText());
        toString(buff, "oldValue", getOldValue());
        toString(buff, "newValue", getNewValue());

        // remove trailing comma
        buff.deleteCharAt(buff.length() - 1);

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
