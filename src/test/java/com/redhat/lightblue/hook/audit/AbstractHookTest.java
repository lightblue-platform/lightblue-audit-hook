/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.redhat.lightblue.hook.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.redhat.lightblue.config.DataSourceConfiguration;
import com.redhat.lightblue.config.DataSourcesConfiguration;
import com.redhat.lightblue.config.LightblueFactory;
import com.redhat.lightblue.metadata.DataStore;
import com.redhat.lightblue.metadata.parser.DataStoreParser;
import com.redhat.lightblue.metadata.parser.Extensions;
import com.redhat.lightblue.metadata.parser.JSONMetadataParser;
import com.redhat.lightblue.metadata.parser.MetadataParser;
import com.redhat.lightblue.metadata.types.DefaultTypes;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 * 
 * @author nmalik
 */
public abstract class AbstractHookTest {

    protected static final String DATASTORE_BACKEND = "mongo";

    // reuse the json node factory, no need to create new ones each test
    private static final JsonNodeFactory nodeFactory = JsonNodeFactory.withExactBigDecimals(false);

    protected AuditHookConfigurationParser hookParser;
    protected JSONMetadataParser parser;

    public static class TestDataStoreParser implements DataStoreParser<JsonNode> {
        @Override
        public DataStore parse(String name, MetadataParser<JsonNode> p, JsonNode node) {
            return new DataStore() {
                @Override
                public String getBackend() {
                    return DATASTORE_BACKEND;
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

    @BeforeClass
    public static void beforeClass() {
        DataSourcesConfiguration dsc = new DataSourcesConfiguration();
        dsc.add(DATASTORE_BACKEND, new DataSourceConfiguration() {

            @Override
            public Class<DataStoreParser> getMetadataDataStoreParser() {
                Class clazz = TestDataStoreParser.class;
                return clazz;
            } 

            @Override
            public void initializeFromJson(JsonNode node) {
            }
        });
        LightblueFactory mgr = new LightblueFactory(dsc);
    }

    @Before
    public void setUp() {
        Extensions<JsonNode> ex = new Extensions<>();
        ex.addDefaultExtensions();
        hookParser = new AuditHookConfigurationParser();
        ex.registerHookConfigurationParser(AuditHook.HOOK_NAME, hookParser);
        ex.registerDataStoreParser(DATASTORE_BACKEND, new TestDataStoreParser());
        parser = new JSONMetadataParser(ex, new DefaultTypes(), nodeFactory);
    }

    @After
    public void tearDown() {
        parser = null;
        hookParser = null;
    }
}
