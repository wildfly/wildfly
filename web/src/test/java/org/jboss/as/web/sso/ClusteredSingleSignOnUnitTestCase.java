package org.jboss.as.web.sso;


import org.jboss.as.clustering.web.sso.SSOClusterManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;

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

    @Before
    public void setup() throws Exception{
        // Set the system properties.
        System.setProperty("org.jboss.as.web.sso.ClusteredSingleSignOn.maxEmptyLife", "200");
        System.setProperty("org.jboss.as.web.sso.ClusteredSingleSignOn.processExpiresInterval", "200");

        final SSOClusterManager clusterManager = PowerMockito.mock(SSOClusterManager.class);

        // Setup the clustered single sign on using a mock cluster manager.
        clusteredSingleSignOn = new ClusteredSingleSignOn(clusterManager);
        clusteredSingleSignOn.start();
    }

    @Test
    public void testProperties() {
        Assert.assertTrue(clusteredSingleSignOn.getMaxEmptyLife() == 200);
        Assert.assertTrue(clusteredSingleSignOn.getProcessExpiresInterval() == 200);
    }

}
