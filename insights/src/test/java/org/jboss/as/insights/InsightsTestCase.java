package org.jboss.as.insights;

import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

import org.jboss.as.insights.extension.InsightsService;
import org.junit.Test;

public class InsightsTestCase {
    
    @Test
    public void testSendJdr() throws IOException {
        String uuid = UUID.randomUUID().toString();
        long tick = InsightsService.DEFAULT_SCHEDULE_INTERVAL;
        boolean enabled = false;
        String insightsEndpoint = InsightsService.DEFAULT_INSIGHTS_ENDPOINT;
        String systemEndpoint = InsightsService.DEFAULT_SYSTEM_ENDPOINT;
        String url = InsightsService.DEFAULT_BASE_URL;
        String userAgent = InsightsService.DEFAULT_USER_AGENT;
//        InsightsService service = InsightsService.getInstance(tick, enabled, insightsEndpoint,
//                systemEndpoint, url, userAgent);
        File testFile = new File("test.txt");
        try (BufferedWriter output = new BufferedWriter(new FileWriter(testFile))) {
            output.write("Lorem Ipsum");
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        
        //TODO: read in rhn login credentials and set them in InsightsService
//        service.sendJdr(testFile.getPath(),uuid);
        //TODO: evaluate if an error was logged in the root logger
        assertTrue(true); //placeholder until test is fully implemented
    }
}
