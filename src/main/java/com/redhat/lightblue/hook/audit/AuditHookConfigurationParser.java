package com.redhat.lightblue.hook.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.lightblue.metadata.HookConfiguration;
import com.redhat.lightblue.metadata.parser.HookConfigurationParser;
import com.redhat.lightblue.metadata.parser.MetadataParser;
import com.redhat.lightblue.util.Error;

/**
 * Parser for AuditHookConfiguration.
 *
 * @author nmalik
 */
public class AuditHookConfigurationParser implements HookConfigurationParser<JsonNode> {
    private static final String PROPERTY_ENTITY_NAME = "entityName";
    private static final String PROPERTY_VERSION = "version";

    private static final String ERR_MISSING_PROPERTY = "hook:MissingProperty";

    @Override
    public HookConfiguration parse(String name, MetadataParser<JsonNode> p, JsonNode node) {
        // note: name is the hook name (comes from generic Parser interface)

        if (node.get(PROPERTY_ENTITY_NAME) == null) {
            throw Error.get(ERR_MISSING_PROPERTY, PROPERTY_ENTITY_NAME);
        }
        if (node.get(PROPERTY_VERSION) == null) {
            throw Error.get(ERR_MISSING_PROPERTY, PROPERTY_VERSION);
        }

        String entityName = node.get("entityName").asText();
        String version = node.get("version").asText();

        return new AuditHookConfiguration(entityName, version);
    }

    @Override
    public void convert(MetadataParser<JsonNode> p, JsonNode emptyNode, HookConfiguration object) {
    }
}
