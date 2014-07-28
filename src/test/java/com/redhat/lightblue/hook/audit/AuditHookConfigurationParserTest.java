package com.redhat.lightblue.hook.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.redhat.lightblue.metadata.DataStore;
import com.redhat.lightblue.metadata.EntityInfo;
import com.redhat.lightblue.metadata.MetadataConstants;
import com.redhat.lightblue.metadata.parser.DataStoreParser;
import com.redhat.lightblue.metadata.parser.Extensions;
import com.redhat.lightblue.metadata.parser.HookConfigurationParser;
import com.redhat.lightblue.metadata.parser.JSONMetadataParser;
import com.redhat.lightblue.metadata.parser.MetadataParser;
import com.redhat.lightblue.metadata.types.DefaultTypes;
import com.redhat.lightblue.util.Error;
import static com.redhat.lightblue.util.JsonUtils.json;
import com.redhat.lightblue.util.test.FileUtil;
import java.io.IOException;
import java.net.URISyntaxException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AuditHookConfigurationParserTest {

    // reuse the json node factory, no need to create new ones each test
    private static final JsonNodeFactory nodeFactory = JsonNodeFactory.withExactBigDecimals(false);

    protected JSONMetadataParser parser;

    public static class TestDataStoreParser implements DataStoreParser<JsonNode> {
        @Override
        public DataStore parse(String name, MetadataParser<JsonNode> p, JsonNode node) {
            return new DataStore() {
                @Override
                public String getBackend() {
                    return "test";
                }
            };
        }

        @Override
        public void convert(MetadataParser<JsonNode> p, JsonNode emptyNode, DataStore object) {
        }

        @Override
        public String getDefaultName() {
            return null;
        }
    }

    @Before
    public void setUp() {
        Extensions<JsonNode> ex = new Extensions<>();
        HookConfigurationParser hookParser = new AuditHookConfigurationParser();
        ex.registerHookConfigurationParser(AuditHookConfigurationParser.HOOK_NAME, hookParser);
        ex.registerDataStoreParser("test", new TestDataStoreParser());
        parser = new JSONMetadataParser(ex, new DefaultTypes(), nodeFactory);
    }

    @After
    public void tearDown() {
        parser = null;
    }

    @Test
    public void testValidJson() throws IOException, URISyntaxException {
        String jsonString = FileUtil.readFile(getClass().getSimpleName() + "-valid.json");

        Assert.assertNotNull(jsonString);

        JsonNode node = json(jsonString);

        EntityInfo entityInfo = parser.parseEntityInfo(node);

        Assert.assertNotNull(entityInfo);
        Assert.assertFalse(entityInfo.getHooks().isEmpty());
        Assert.assertEquals(AuditHookConfigurationParser.HOOK_NAME, entityInfo.getHooks().getHooks().get(0).getName());

        // verify configuration
        Assert.assertEquals("audit", ((AuditHookConfiguration) entityInfo.getHooks().getHooks().get(0).getConfiguration()).getEntityName());
        Assert.assertEquals("1.0.0", ((AuditHookConfiguration) entityInfo.getHooks().getHooks().get(0).getConfiguration()).getVersion());

    }

    @Test
    public void testMissingVersion() throws IOException, URISyntaxException {
        String jsonString = FileUtil.readFile(getClass().getSimpleName() + "-missing-version.json");

        Assert.assertNotNull(jsonString);

        JsonNode node = json(jsonString);

        try {
            EntityInfo entityInfo = parser.parseEntityInfo(node);
            Assert.fail("Expected Error to be thrown");
        } catch (Error e) {
            Assert.assertEquals(MetadataConstants.ERR_PARSE_MISSING_ELEMENT, e.getErrorCode());
            Assert.assertEquals(AuditHookConfigurationParser.PROPERTY_VERSION, e.getMsg());
        }
    }

    @Test
    public void testMissingEntityName() throws IOException, URISyntaxException {
        String jsonString = FileUtil.readFile(getClass().getSimpleName() + "-missing-entityName.json");

        Assert.assertNotNull(jsonString);

        JsonNode node = json(jsonString);

        try {
            EntityInfo entityInfo = parser.parseEntityInfo(node);
            Assert.fail("Expected Error to be thrown");
        } catch (Error e) {
            Assert.assertEquals(MetadataConstants.ERR_PARSE_MISSING_ELEMENT, e.getErrorCode());
            Assert.assertEquals(AuditHookConfigurationParser.PROPERTY_ENTITY_NAME, e.getMsg());
        }
    }

    @Test
    public void testMissingConfiguration() throws IOException, URISyntaxException {
        String jsonString = FileUtil.readFile(getClass().getSimpleName() + "-missing-configuration.json");

        Assert.assertNotNull(jsonString);

        JsonNode node = json(jsonString);

        EntityInfo entityInfo = parser.parseEntityInfo(node);

        // simply won't have configuration on the hook
        Assert.assertFalse(entityInfo.getHooks().isEmpty());
        Assert.assertEquals(AuditHookConfigurationParser.HOOK_NAME, entityInfo.getHooks().getHooks().get(0).getName());
        Assert.assertNull(entityInfo.getHooks().getHooks().get(0).getConfiguration());
    }
}
