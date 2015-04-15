package com.redhat.lightblue.hook.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.lightblue.metadata.HookConfiguration;
import com.redhat.lightblue.metadata.parser.HookConfigurationParser;
import com.redhat.lightblue.metadata.parser.MetadataParser;

/**
 * Parser for AuditHookConfiguration.
 *
 * @author nmalik
 */
public class AuditHookConfigurationParser<T> implements HookConfigurationParser<T> {
    public static final String PROPERTY_ENTITY_NAME = "entityName";
    public static final String PROPERTY_VERSION = "version";

    @Override
    public String getName() {
        return AuditHook.HOOK_NAME;
    }

    @Override
    public HookConfiguration parse(String name, MetadataParser<T> p, T node) {
        // note: name is the hook name (comes from generic Parser interface)
        String entityName = p.getRequiredStringProperty(node, PROPERTY_ENTITY_NAME);
        String version = p.getRequiredStringProperty(node, PROPERTY_VERSION);

        return new AuditHookConfiguration(entityName, version);
    }

    @Override
    public void convert(MetadataParser<T> p, T emptyNode, HookConfiguration object) {
        if (object instanceof AuditHookConfiguration) {
            AuditHookConfiguration ahc = (AuditHookConfiguration) object;
            p.putValue(emptyNode, PROPERTY_ENTITY_NAME, ahc.getEntityName());
            p.putValue(emptyNode, PROPERTY_VERSION, ahc.getVersion());
        }
    }
}
