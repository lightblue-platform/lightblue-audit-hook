package com.redhat.lightblue.hook.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.redhat.lightblue.metadata.DataStore;
import com.redhat.lightblue.metadata.EntityInfo;
import com.redhat.lightblue.metadata.parser.DataStoreParser;
import com.redhat.lightblue.metadata.parser.Extensions;
import com.redhat.lightblue.metadata.parser.HookConfigurationParser;
import com.redhat.lightblue.metadata.parser.JSONMetadataParser;
import com.redhat.lightblue.metadata.parser.MetadataParser;
import com.redhat.lightblue.metadata.types.DefaultTypes;
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

    // must be same as what is in JSON
    protected static final String HOOK_NAME = "auditHook";

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
        ex.registerHookConfigurationParser(HOOK_NAME, hookParser);
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
        Assert.assertEquals(HOOK_NAME, entityInfo.getHooks().getHooks().get(0).getName());

        // verify configuration
        Assert.assertEquals("audit", ((AuditHookConfiguration) entityInfo.getHooks().getHooks().get(0).getConfiguration()).getEntityName());
        Assert.assertEquals("1.0.0", ((AuditHookConfiguration) entityInfo.getHooks().getHooks().get(0).getConfiguration()).getVersion());

    }
}
