package org.jboss.as.web.sso;


import org.jboss.as.clustering.web.sso.SSOClusterManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;

import java.util.Properties;

/**
 * Unit test to check the ability to set the maxEmptyLife and processExpiresInterval values as system properties.
 *
 * This is a unit test written specifically for BZ:958572.
 *
 * @author navssurtani
 */
public class ClusteredSingleSignOnUnitTestCase {

    // The ClusteredSSO Object.
    ClusteredSingleSignOn clusteredSingleSignOn;

    private static final String MAX_EMPTY_LIFE_PROPERTY = "org.jboss.as.web.sso.ClusteredSingleSignOn.maxEmptyLife";
    private static final String PROCESS_EXPIRES_INTERVAL = "org.jboss.as.web.sso.ClusteredSingleSignOn" +
          ".processExpiresInterval";

    @Before
    public void setup() throws Exception{
        // Set the system properties.
        System.setProperty(MAX_EMPTY_LIFE_PROPERTY, "200");
        System.setProperty(PROCESS_EXPIRES_INTERVAL, "200");

        final SSOClusterManager clusterManager = PowerMockito.mock(SSOClusterManager.class);

        // Setup the clustered single sign on using a mock cluster manager.
        clusteredSingleSignOn = new ClusteredSingleSignOn(clusterManager);
        clusteredSingleSignOn.start();
    }

    @After
    public void clearUp() throws Exception {
        // Remove the previously set property
        System.clearProperty(MAX_EMPTY_LIFE_PROPERTY);
        System.clearProperty(PROCESS_EXPIRES_INTERVAL);
    }

    @Test
    public void testProperties() {
        Assert.assertTrue(clusteredSingleSignOn.getMaxEmptyLife() == 200);
        Assert.assertTrue(clusteredSingleSignOn.getProcessExpiresInterval() == 200);
    }

}
