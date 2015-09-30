package com.redhat.lightblue.hook.audit;

import static com.redhat.lightblue.util.JsonUtils.json;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.lightblue.metadata.EntityInfo;
import com.redhat.lightblue.util.test.FileUtil;

public class AuditHookConfigurationParserTest extends AbstractHookTest {

    @Test
    public void getName() {
        Assert.assertEquals(AuditHook.HOOK_NAME, hookParser.getName());
    }

    @Test
    public void testMissingConfiguration() throws IOException, URISyntaxException {
        String jsonString = FileUtil.readFile(getClass().getSimpleName() + "-missing-configuration.json");

        Assert.assertNotNull(jsonString);

        JsonNode node = json(jsonString);

        EntityInfo entityInfo = parser.parseEntityInfo(node);

        // simply won't have configuration on the hook
        Assert.assertFalse(entityInfo.getHooks().isEmpty());
        Assert.assertEquals(AuditHook.HOOK_NAME, entityInfo.getHooks().getHooks().get(0).getName());
        Assert.assertNull(entityInfo.getHooks().getHooks().get(0).getConfiguration());
    }

    @Test
    public void testValidJson() throws IOException, URISyntaxException {
        String jsonString = FileUtil.readFile(getClass().getSimpleName() + "-valid.json");

        Assert.assertNotNull(jsonString);

        JsonNode node = json(jsonString);

        EntityInfo entityInfo = parser.parseEntityInfo(node);

        Assert.assertNotNull(entityInfo);
        Assert.assertFalse(entityInfo.getHooks().isEmpty());
        Assert.assertEquals(AuditHook.HOOK_NAME, entityInfo.getHooks().getHooks().get(0).getName());
    }

    @Test
    public void parse() throws IOException, URISyntaxException {
        String jsonString = "{\"entityName\":\"audit\",\"version\":\"1.0.0\"}";

        AuditHookConfigurationParser<JsonNode> p = new AuditHookConfigurationParser<JsonNode>();

        AuditHookConfiguration config = (AuditHookConfiguration) p.parse("audit", parser, json(jsonString));

        Assert.assertNotNull(config);
    }

    @Test
    public void convert() throws IOException, URISyntaxException {
        String jsonString = "{\"entityName\":\"audit\",\"version\":\"1.0.0\"}";

        AuditHookConfigurationParser<JsonNode> p = new AuditHookConfigurationParser<JsonNode>();

        AuditHookConfiguration config = (AuditHookConfiguration) p.parse("audit", parser, json(jsonString));

        JsonNode node = parser.newNode();

        p.convert(parser, node, config);

        Assert.assertFalse(node.elements().hasNext());
    }

    @Override
    protected String[] getMetadataResources() {
        // there is no metadata for this test..
        return new String[]{"metadata/audit.json"};
    }
}
