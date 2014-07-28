package com.redhat.lightblue.hook.audit;

import com.redhat.lightblue.util.test.FileUtil;
import java.io.IOException;
import java.net.URISyntaxException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AuditHookConfigurationParserTest {
    private AuditHookConfigurationParser parser;
    
    @Before
    public void setUp() {
        parser = new AuditHookConfigurationParser();
    }
    
    @After
    public void tearDown() {
        parser = null;
    }
    
    @Test
    public void testValidJson() throws IOException, URISyntaxException {
        String jsonString = FileUtil.readFile(getClass().getSimpleName() + "-valid.json");
        
        Assert.assertNotNull(jsonString);
    }
}
