package com.redhat.lightblue.hook.audit.integration;

import static com.redhat.lightblue.test.Assert.assertNoDataErrors;
import static com.redhat.lightblue.test.Assert.assertNoErrors;
import static com.redhat.lightblue.util.JsonUtils.json;
import static org.junit.Assert.assertEquals;

import java.util.Iterator;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.lightblue.Response;
import com.redhat.lightblue.crud.DeleteRequest;
import com.redhat.lightblue.crud.FindRequest;
import com.redhat.lightblue.crud.InsertionRequest;
import com.redhat.lightblue.crud.UpdateRequest;
import com.redhat.lightblue.mongo.test.AbstractMongoCRUDTestController;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ITAuditHook extends AbstractMongoCRUDTestController {

    private static final String AUDIT_VERSION = "1.0.1";
    private static final String COUNTRY_VERSION = "0.1.0-SNAPSHOT";

    @BeforeClass
    public static void prepareAuditHookDatasources() {
        System.setProperty("mongo.datasource", "mongo");
    }

    public ITAuditHook() throws Exception {
        super();
    }

    @Override
    protected JsonNode[] getMetadataJsonNodes() throws Exception {
        return new JsonNode[]{
                json(loadResource("/metadata/audit.json", true)),
                json(loadResource("/metadata/country_with_hooks.json", true))
        };
    }

    @Test
    public void test1InsertCreatesAudits() throws Exception {
        Response insertResponse = getLightblueFactory().getMediator().insert(createRequest_FromJsonString(
                InsertionRequest.class,
                "{\"entity\":\"country\",\"entityVersion\":\"" + COUNTRY_VERSION + "\",\"data\":["
                        + "{\"name\":\"United States\",\"iso2Code\":\"123\",\"iso3Code\":\"456\"},"
                        + "{\"name\":\"Mexico\",\"iso2Code\":\"qaz\",\"iso3Code\":\"zaq\"},"
                        + "{\"name\":\"Canada\",\"iso2Code\":\"abc\",\"iso3Code\":\"def\"}"
                        + "]}"));
        assertNoErrors(insertResponse);
        assertNoDataErrors(insertResponse);
        assertEquals(3, insertResponse.getModifiedCount());

        Response findResponse = getLightblueFactory().getMediator().find(createRequest_FromJsonString(
                FindRequest.class,
                "{\"entity\":\"audit\",\"entityVersion\":\"" + AUDIT_VERSION + "\","
                        + "\"query\":{\"field\":\"objectType\",\"op\":\"$eq\",\"rvalue\":\"audit\"},"
                        + "\"projection\": [{\"field\":\"*\",\"include\":true,\"recursive\":true}]}"));
        assertNoErrors(findResponse);
        assertNoDataErrors(findResponse);
        assertEquals(3, findResponse.getMatchCount());

        Iterator<JsonNode> auditEntries = findResponse.getEntityData().iterator();
        while (auditEntries.hasNext()) {
            JsonNode auditEntry = auditEntries.next();
            assertEquals("INSERT", auditEntry.get("CRUDOperation").asText());
        }
    }

    @Test
    public void test2UpdateCreatesAudits() throws Exception {
        Response updateResponse = getLightblueFactory().getMediator().update(createRequest_FromJsonString(
                UpdateRequest.class,
                "{\"entity\":\"country\",\"entityVersion\":\"" + COUNTRY_VERSION + "\","
                        + "\"projection\": [{\"field\":\"*\",\"include\":true,\"recursive\":true}],"
                        + "\"query\":{\"field\":\"name\",\"op\":\"$in\",\"values\":[\"United States\",\"Canada\"]},"
                        + "\"update\":["
                        + "{\"$set\":{\"optionalField\":\"modified\"}}"
                        + "]}"));
        assertNoErrors(updateResponse);
        assertNoDataErrors(updateResponse);
        assertEquals(2, updateResponse.getModifiedCount());

        Response findResponse = getLightblueFactory().getMediator().find(createRequest_FromJsonString(
                FindRequest.class,
                "{\"entity\":\"audit\",\"entityVersion\":\"" + AUDIT_VERSION + "\","
                        + "\"query\":{\"field\":\"CRUDOperation\",\"op\":\"$eq\",\"rvalue\":\"UPDATE\"},"
                        + "\"projection\": [{\"field\":\"*\",\"include\":true,\"recursive\":true}]}"));
        assertNoErrors(findResponse);
        assertNoDataErrors(findResponse);
        assertEquals(2, findResponse.getMatchCount());
    }

    @Test
    public void test3DeleteCreatesAudits() throws Exception {
        Response deleteResponse = getLightblueFactory().getMediator().delete(createRequest_FromJsonString(
                DeleteRequest.class,
                "{\"entity\":\"country\",\"entityVersion\":\"" + COUNTRY_VERSION + "\","
                        + "\"projection\": [{\"field\":\"*\",\"include\":true,\"recursive\":true}],"
                        + "\"query\":{\"field\":\"name\",\"op\":\"$in\",\"values\":[\"United States\",\"Canada\"]}}"));
        assertNoErrors(deleteResponse);
        assertNoDataErrors(deleteResponse);
        assertEquals(2, deleteResponse.getModifiedCount());

        Response findResponse = getLightblueFactory().getMediator().find(createRequest_FromJsonString(
                FindRequest.class,
                "{\"entity\":\"audit\",\"entityVersion\":\"" + AUDIT_VERSION + "\","
                        + "\"query\":{\"field\":\"CRUDOperation\",\"op\":\"$eq\",\"rvalue\":\"DELETE\"},"
                        + "\"projection\": [{\"field\":\"*\",\"include\":true,\"recursive\":true}]}"));
        assertNoErrors(findResponse);
        assertNoDataErrors(findResponse);
        assertEquals(2, findResponse.getMatchCount());
    }
}
