package com.redhat.lightblue.hook.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.main.JsonSchema;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.redhat.lightblue.crud.Operation;
import com.redhat.lightblue.hook.audit.model.Audit;
import com.redhat.lightblue.hooks.HookDoc;
import com.redhat.lightblue.metadata.EntityMetadata;
import com.redhat.lightblue.metadata.Field;
import com.redhat.lightblue.util.JsonDoc;
import com.redhat.lightblue.util.JsonUtils;
import static com.redhat.lightblue.util.JsonUtils.json;
import com.redhat.lightblue.util.test.FileUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test AuditHook with country.json.
 *
 * @author nmalik
 */
public class CountryAuditHookTest extends AbstractHookTest {
    protected static final String COUNTRY_METADATA_FILENAME = "metadata/country.json";

    /**
     * Verify country.json against metadata json schema.
     *
     * @throws Exception
     */
    @Test
    public void jsonSchema() throws Exception {
        String jsonSchemaString = FileUtil.readFile(COUNTRY_METADATA_FILENAME);

        JsonNode node = json(jsonSchemaString);

        JsonSchema schema = JsonUtils.loadSchema("json-schema/metadata/metadata.json");
        // verify metadata is valid
        String report = JsonUtils.jsonSchemaValidation(schema, node);

        // if report isn't null it's a failure and the value of report is the detail of why
        Assert.assertTrue("Expected validation to succeed!\nResource: " + COUNTRY_METADATA_FILENAME + "\nMessages:\n" + report, report == null);
    }

    /**
     * Verify parsing of country.json into EntityMetadata.
     *
     * @throws Exception
     */
    @Test
    public void entityMetadata() throws Exception {
        String jsonSchemaString = FileUtil.readFile(COUNTRY_METADATA_FILENAME);

        EntityMetadata em = parser.parseEntityMetadata(json(jsonSchemaString));

        Assert.assertNotNull(em);
    }

    /**
     * Verify identifying fields found are iso2Code and iso3Code.
     *
     * @throws Exception
     */
    @Test
    public void identifyingFields() throws Exception {
        String jsonSchemaString = FileUtil.readFile(COUNTRY_METADATA_FILENAME);
        EntityMetadata em = parser.parseEntityMetadata(json(jsonSchemaString));

        Field[] identifyingFields = em.getEntitySchema().getIdentityFields();
        Assert.assertNotNull(identifyingFields);
        Assert.assertEquals(2, identifyingFields.length);
        Set<String> identifyingFieldPaths = new HashSet<>();
        for (Field f : identifyingFields) {
            identifyingFieldPaths.add(f.getFullPath().toString());
        }
        Assert.assertTrue(identifyingFieldPaths.contains("iso2Code"));
        Assert.assertTrue(identifyingFieldPaths.contains("iso3Code"));
        Assert.assertFalse(identifyingFieldPaths.contains("name"));
    }

    /**
     * Verify one missing identifying field causes AuditHook to fail.
     *
     * @throws Exception
     */
    @Test
    public void missingOneIdentifyingField() throws Exception {
        String jsonSchemaString = FileUtil.readFile(COUNTRY_METADATA_FILENAME);
        EntityMetadata em = parser.parseEntityMetadata(json(jsonSchemaString));

        // create hook configuration
        AuditHookConfiguration config = new AuditHookConfiguration(em.getName(), em.getVersion().getValue());

        // ------------------------------------------------------------
        // mock up document data
        List<HookDoc> processedDocuments = new ArrayList<>();

        // need a json node for pre and post data.  will create together to make easier
        JsonNode pre = json(FileUtil.readFile(getClass().getSimpleName() + "-missingOneIdentifyingField-pre.json"));
        JsonNode post = json(FileUtil.readFile(getClass().getSimpleName() + "-missingOneIdentifyingField-post.json"));

        HookDoc hd = new HookDoc(em, new JsonDoc(pre), new JsonDoc(post), Operation.UPDATE);

        processedDocuments.add(hd);
        // ------------------------------------------------------------

        // create hook
        AuditHook hook = new AuditHook();

        // process hook
        try {
            hook.processHook(em, config, processedDocuments);
            Assert.fail("Expected Error to be thrown!");
        } catch (com.redhat.lightblue.util.Error e) {
            Assert.assertEquals(AuditHook.ERR_MISSING_ID, e.getErrorCode());
        }
    }

    /**
     * Verify missing all identifying field causes AuditHook to fail.
     *
     * @throws Exception
     */
    @Test
    public void missingAllIdentifyingFields() throws Exception {
        String jsonSchemaString = FileUtil.readFile(COUNTRY_METADATA_FILENAME);
        EntityMetadata em = parser.parseEntityMetadata(json(jsonSchemaString));

        // create hook configuration
        AuditHookConfiguration config = new AuditHookConfiguration(em.getName(), em.getVersion().getValue());

        // ------------------------------------------------------------
        // mock up document data
        List<HookDoc> processedDocuments = new ArrayList<>();

        // need a json node for pre and post data.  will create together to make easier
        JsonNode pre = json(FileUtil.readFile(getClass().getSimpleName() + "-missingAllIdentifyingFields-pre.json"));
        JsonNode post = json(FileUtil.readFile(getClass().getSimpleName() + "-missingAllIdentifyingFields-post.json"));

        HookDoc hd = new HookDoc(em, new JsonDoc(pre), new JsonDoc(post), Operation.UPDATE);

        processedDocuments.add(hd);
        // ------------------------------------------------------------

        // create hook
        AuditHook hook = new AuditHook();

        // process hook
        try {
            hook.processHook(em, config, processedDocuments);
            Assert.fail("Expected Error to be thrown!");
        } catch (com.redhat.lightblue.util.Error e) {
            Assert.assertEquals(AuditHook.ERR_MISSING_ID, e.getErrorCode());
        }
    }

    /**
     * Verify with no missing identifying fields, hook should be successful.
     *
     * @throws Exception
     */
    @Test
    public void noMissingIdentifyingFields() throws Exception {
        // verify up front there is nothing in audit collection
        DBCollection auditColl = db.createCollection("audit", null);
        Assert.assertEquals(0, auditColl.find().count());

        String jsonSchemaString = FileUtil.readFile(COUNTRY_METADATA_FILENAME);
        EntityMetadata em = parser.parseEntityMetadata(json(jsonSchemaString));

        // create hook configuration
        AuditHookConfiguration config = new AuditHookConfiguration(em.getName(), em.getVersion().getValue());

        // ------------------------------------------------------------
        // mock up document data
        List<HookDoc> processedDocuments = new ArrayList<>();

        // need a json node for pre and post data.  will create together to make easier
        JsonNode pre = json(FileUtil.readFile(getClass().getSimpleName() + "-noMissingIdentifyingFields-pre.json"));
        JsonNode post = json(FileUtil.readFile(getClass().getSimpleName() + "-noMissingIdentifyingFields-post.json"));

        HookDoc hd = new HookDoc(em, new JsonDoc(pre), new JsonDoc(post), Operation.UPDATE);

        processedDocuments.add(hd);
        // ------------------------------------------------------------

        // create hook
        AuditHook hook = new AuditHook();

        // process hook
        hook.processHook(em, config, processedDocuments);

        // verify data was persisted
        Assert.assertEquals(1, auditColl.find().count());
    }

    /**
     * Check side effect when there is a difference between pre and post
     * documents.
     *
     * @throws Exception
     */
    @Test
    public void differentPreAndPost() throws Exception {
        // verify up front there is nothing in audit collection
        DBCollection auditColl = db.createCollection("audit", null);
        Assert.assertEquals(0, auditColl.find().count());

        String jsonSchemaString = FileUtil.readFile(COUNTRY_METADATA_FILENAME);
        EntityMetadata em = parser.parseEntityMetadata(json(jsonSchemaString));

        // create hook configuration
        AuditHookConfiguration config = new AuditHookConfiguration(em.getName(), em.getVersion().getValue());

        // ------------------------------------------------------------
        // mock up document data
        List<HookDoc> processedDocuments = new ArrayList<>();

        // need a json node for pre and post data.  will create together to make easier
        JsonNode pre = json(FileUtil.readFile(getClass().getSimpleName() + "-differentPreAndPost-pre.json"));
        JsonNode post = json(FileUtil.readFile(getClass().getSimpleName() + "-differentPreAndPost-post.json"));

        HookDoc hd = new HookDoc(em, new JsonDoc(pre), new JsonDoc(post), Operation.UPDATE);

        processedDocuments.add(hd);
        // ------------------------------------------------------------

        // create hook
        AuditHook hook = new AuditHook();

        // process hook
        hook.processHook(em, config, processedDocuments);

        // verify data persisted
        Assert.assertEquals(1, auditColl.find().count());

        DBObject dbobj = auditColl.findOne();
        BasicDBList data = ((BasicDBList) dbobj.get("audits"));

        Assert.assertFalse(data.isEmpty());
        Assert.assertEquals(1, data.size());
        
        Assert.assertEquals("optionalField", ((BasicDBObject)data.get(0)).get("fieldText").toString());
        Assert.assertEquals("changed", ((BasicDBObject)data.get(0)).get("newValue").toString());
    }

    /**
     * Verify nothing is auditable if nothing changes.
     *
     * @throws Exception
     */
    @Test
    public void nothingToAudit() throws Exception {
        // verify up front there is nothing in audit collection
        DBCollection auditColl = db.createCollection("audit", null);
        Assert.assertEquals(0, auditColl.find().count());

        String jsonSchemaString = FileUtil.readFile(COUNTRY_METADATA_FILENAME);
        EntityMetadata em = parser.parseEntityMetadata(json(jsonSchemaString));

        // create hook configuration
        AuditHookConfiguration config = new AuditHookConfiguration(em.getName(), em.getVersion().getValue());

        // ------------------------------------------------------------
        // mock up document data
        List<HookDoc> processedDocuments = new ArrayList<>();

        // need a json node for pre and post data.  will create together to make easier
        JsonNode pre = json(FileUtil.readFile(getClass().getSimpleName() + "-nothingToAudit-pre.json"));
        JsonNode post = json(FileUtil.readFile(getClass().getSimpleName() + "-nothingToAudit-post.json"));

        HookDoc hd = new HookDoc(em, new JsonDoc(pre), new JsonDoc(post), Operation.UPDATE);

        processedDocuments.add(hd);
        // ------------------------------------------------------------

        // create hook
        AuditHook hook = new AuditHook();

        // process hook
        hook.processHook(em, config, processedDocuments);

        // and verify there is no audit data found for the one document.
        Audit audit = hook.findAuditFor(em, config, hd);

        Assert.assertNull(audit);

        // verify audit collection still empty
        Assert.assertEquals(0, auditColl.find().count());
    }

    @Override
    protected String[] getMetadataResources() {
        return new String[]{"metadata/audit.json","metadata/country.json"};
    }
}
