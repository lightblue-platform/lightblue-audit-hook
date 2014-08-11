/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.redhat.lightblue.hook.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.main.JsonSchema;
import com.redhat.lightblue.crud.Operation;
import com.redhat.lightblue.hooks.HookDoc;
import com.redhat.lightblue.metadata.EntityMetadata;
import com.redhat.lightblue.util.JsonDoc;
import com.redhat.lightblue.util.JsonUtils;
import static com.redhat.lightblue.util.JsonUtils.json;
import com.redhat.lightblue.util.test.FileUtil;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author nmalik
 */
public class AuditHookTest extends AbstractHookTest {
    protected static final String AUDIT_METADATA_FILENAME = "metadata/audit.json";

    /**
     * Verify the audit metadata conforms to metadata schema.
     *
     * @throws Exception
     */
    @Test
    public void verifyMetadataJson() throws Exception {
        String jsonSchemaString = FileUtil.readFile(AUDIT_METADATA_FILENAME);

        JsonNode node = json(jsonSchemaString);

        JsonSchema schema = JsonUtils.loadSchema("json-schema/metadata/metadata.json");
        // verify metadata is valid
        String report = JsonUtils.jsonSchemaValidation(schema, node);

        // if report isn't null it's a failure and the value of report is the detail of why
        Assert.assertTrue("Expected validation to succeed!\nResource: " + AUDIT_METADATA_FILENAME + "\nMessages:\n" + report, report == null);
    }

    /**
     * Verify audit metadata can be parsed into EntityMetadata object.
     *
     * @throws Exception
     */
    @Test
    public void verifyEntityMetadata() throws Exception {
        // load
        String jsonSchemaString = FileUtil.readFile(AUDIT_METADATA_FILENAME);

        // parser
        EntityMetadata em = parser.parseEntityMetadata(json(jsonSchemaString));

        // verify (simple)
        Assert.assertNotNull(em);
    }

    /**
     * Verify name returned by audit hook.
     */
    @Test
    public void getName() throws Exception {
        AuditHook hook = new AuditHook();

        Assert.assertEquals(AuditHook.HOOK_NAME, hook.getName());
    }

    /**
     * Very simple (and hacky) test of document processing.
     *
     * @throws Exception
     */
    @Test
    public void updateWithId() throws Exception {
        // load
        String jsonSchemaString = FileUtil.readFile(AUDIT_METADATA_FILENAME);

        // parser
        EntityMetadata em = parser.parseEntityMetadata(json(jsonSchemaString));

        // create hook configuration
        AuditHookConfiguration config = new AuditHookConfiguration("test", "1.0.0");

        // ------------------------------------------------------------
        // mock up document data
        List<HookDoc> processedDocuments = new ArrayList<>();

        // need a json node for pre and post data.  will create together to make easier
        ObjectNode pre = new ObjectNode(JsonNodeFactory.instance);
        ObjectNode post = new ObjectNode(JsonNodeFactory.instance);

        // only set _id on post, assumes the update input doesn't have the _id
        post.put("_id", "ID");

        // will have a field "foo" on each with different values
        pre.put("foo", "old");
        post.put("foo", "new");

        // and field "bar" with same values
        pre.put("bar", "same");
        post.put("bar", "same");

        HookDoc hd = new HookDoc(em, new JsonDoc(pre), new JsonDoc(post), Operation.UPDATE);

        processedDocuments.add(hd);
        // ------------------------------------------------------------

        // create hook
        AuditHook hook = new AuditHook();

        // process hook
        hook.processHook(em, config, processedDocuments);
    }
}
