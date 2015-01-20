/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.redhat.lightblue.hook.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.redhat.lightblue.config.DataSourcesConfiguration;
import com.redhat.lightblue.metadata.EntityMetadata;
import com.redhat.lightblue.metadata.mongo.MongoDataStoreParser;
import com.redhat.lightblue.metadata.parser.Extensions;
import com.redhat.lightblue.metadata.parser.JSONMetadataParser;
import com.redhat.lightblue.metadata.types.DefaultTypes;
import com.redhat.lightblue.mongo.config.MongoConfiguration;
import com.redhat.lightblue.mongo.test.EmbeddedMongo;
import com.redhat.lightblue.util.JsonUtils;
import com.redhat.lightblue.util.test.FileUtil;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import org.junit.After;

/**
 * Abstract hook test assuming use of mongo backend and metadata for test as
 * resource(s) on classpath.
 *
 * @author nmalik
 */
public abstract class AbstractHookTest {

    protected static EmbeddedMongo mongo = EmbeddedMongo.getInstance();
    protected static final String DATASTORE_BACKEND = "mongo";

    // reuse the json node factory, no need to create new ones each test
    private static final JsonNodeFactory NODE_FACTORY = JsonNodeFactory.withExactBigDecimals(false);

    protected static AuditHookConfigurationParser hookParser;
    protected static JSONMetadataParser parser;



    @BeforeClass
    public static void beforeClass() throws Exception {
        AbstractMongoTest.setupClass();
        DataSourcesConfiguration dsc = new DataSourcesConfiguration();
        dsc.add(DATASTORE_BACKEND, new MongoConfiguration());
    }

    @BeforeClass
    public static void setupClass() throws Exception {
        // prepare parsers
        Extensions<JsonNode> ex = new Extensions<>();
        ex.addDefaultExtensions();
        hookParser = new AuditHookConfigurationParser();
        ex.registerHookConfigurationParser(AuditHook.HOOK_NAME, hookParser);
        ex.registerDataStoreParser(DATASTORE_BACKEND, new MongoDataStoreParser());
        parser = new JSONMetadataParser(ex, new DefaultTypes(), NODE_FACTORY);
    }

    @Before
    public void setup()  {
        // create metadata
        try {
            for (String resource : getMetadataResources()) {
                String jsonString = null;
                jsonString = FileUtil.readFile(resource);
                EntityMetadata em = parser.parseEntityMetadata(JsonUtils.json(jsonString));
                AuditHook.getFactory().getMetadata().createNewMetadata(em);
            }
         } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @After
    public void teardown() throws Exception {
        mongo.reset();
    }

    @AfterClass
    public static void teardownClass() throws Exception {
        parser = null;
        hookParser = null;
    }

    /**
     * Get list of resources on classpath representing metadata for the test
     * implementation.
     *
     * @return array of resource names
     */
    protected abstract String[] getMetadataResources();
}
