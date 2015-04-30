package com.redhat.lightblue.hook.audit.integration;

import static com.redhat.lightblue.test.Assert.assertNoDataErrors;
import static com.redhat.lightblue.test.Assert.assertNoErrors;
import static com.redhat.lightblue.util.JsonUtils.json;
import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.lightblue.Response;
import com.redhat.lightblue.crud.FindRequest;
import com.redhat.lightblue.crud.InsertionRequest;
import com.redhat.lightblue.mongo.test.AbstractMongoCRUDTestController;

public class ITAuditHook extends AbstractMongoCRUDTestController {

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
    public void test() throws Exception {
        Response insertResponse = getLightblueFactory().getMediator().insert(createRequest_FromJsonString(
                InsertionRequest.class,
                "{\"entity\":\"country\",\"entityVersion\":\"0.1.0-SNAPSHOT\",\"data\":["
                        + "{\"name\":\"United States\",\"iso2Code\":\"123\",\"iso3Code\":\"456\"},"
                        + "{\"name\":\"Canada\",\"iso2Code\":\"abc\",\"iso3Code\":\"def\"}"
                        + "]}"));
        assertNoErrors(insertResponse);
        assertNoDataErrors(insertResponse);
        assertEquals(2, insertResponse.getModifiedCount());

        Response findResponse = getLightblueFactory().getMediator().find(createRequest_FromJsonString(
                FindRequest.class,
                "{\"entity\":\"audit\",\"entityVersion\":\"1.0.1\","
                        + "\"query\":{\"field\":\"objectType\",\"op\":\"$eq\",\"rvalue\":\"audit\"},"
                        + "\"projection\": [{\"field\":\"*\",\"include\":true,\"recursive\":true}]}"));
        assertNoErrors(findResponse);
        assertNoDataErrors(findResponse);
        assertEquals(2, findResponse.getMatchCount());
    }
}
