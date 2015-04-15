package com.redhat.lightblue.hook.audit;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.redhat.lightblue.mongo.test.EmbeddedMongo;
import com.redhat.lightblue.util.test.AbstractJsonSchemaTest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;


/**
 * Created by lcestari on 1/9/15.
 */
public abstract class AbstractMongoTest extends AbstractJsonSchemaTest {
    private static EmbeddedMongo mongo = EmbeddedMongo.getInstance();
    protected static final String COLL_NAME = "data";
    protected static DB db;
    protected DBCollection coll;

    protected final String key1 = "name";
    protected final String key2 = "foo";

    @BeforeClass
    public static void setupClass() throws Exception {
        db = mongo.getDB();
    }

    @Before
    public void setup() {
        coll = db.getCollection(COLL_NAME);

        // setup data
        int count = 0;
        for (int i = 1; i < 5; i++) {
            for (int x = 1; x < i + 1; x++) {
                DBObject obj = new BasicDBObject(key1, "obj" + i);
                obj.put(key2, "bar" + x);
                coll.insert(obj);
                count++;
            }
        }

        Assert.assertEquals(count, coll.find().count());
    }

    @After
    public void teardown() {
        mongo.reset();
        coll = null;
    }
}