package org.jboss.as.test.integration.ee.appclient.basic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.ee.appclient.util.AppClientWrapper;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.StatelessEJBLocator;
import org.jboss.shrinkwrap.api.Archive;

public abstract class AbstractSimpleApplicationClientTestCase {

    protected static final String APP_NAME = "simple-app-client-test";

    protected static final String MODULE_NAME = "ejb";

    @ArquillianResource
    protected ManagementClient managementClient;

    public abstract Archive<?> getArchive();

    /**
     * Tests a simple app client that calls an ejb with its command line parameters
     */
    public void simpleAppClientTest() throws Exception {
        final StatelessEJBLocator<AppClientSingletonRemote> locator = new StatelessEJBLocator(AppClientSingletonRemote.class,
                APP_NAME, MODULE_NAME, AppClientStateSingleton.class.getSimpleName(), "");
        final AppClientSingletonRemote remote = EJBClient.createProxy(locator);
        remote.reset();
        final AppClientWrapper wrapper = new AppClientWrapper(getArchive(), "--host=" + managementClient.getRemoteEjbURL(),
                "client-annotation.jar", "cmdLineParam");
        try {
            final String result = remote.awaitAppClientCall();
            assertTrue("App client call failed. App client output: " + wrapper.readAllUnformated(1000), result != null);
            assertEquals("cmdLineParam", result);
        } finally {
            wrapper.quit();
        }
    }

    /**
     * Tests an app client with a deployment descriptor, that injects an env-entry and an EJB.
     *
     * @throws Exception
     */
    public void descriptorBasedAppClientTest() throws Exception {
        final StatelessEJBLocator<AppClientSingletonRemote> locator = new StatelessEJBLocator(AppClientSingletonRemote.class,
                APP_NAME, MODULE_NAME, AppClientStateSingleton.class.getSimpleName(), "");
        final AppClientSingletonRemote remote = EJBClient.createProxy(locator);
        remote.reset();
        final AppClientWrapper wrapper = new AppClientWrapper(getArchive(), "--host=" + managementClient.getRemoteEjbURL(),
                "client-dd.jar", "");
        try {
            final String result = remote.awaitAppClientCall();
            assertTrue("App client call failed. App client output: " + wrapper.readAllUnformated(1000), result != null);
            assertEquals("EnvEntry", result);
        } finally {
            wrapper.quit();
        }
    }

    /**
     * Tests an app client with a deployment descriptor, that injects an env-entry and an EJB.
     *
     * @throws Exception
     */
    public void testAppClientJBossDescriptor() throws Exception {
        final StatelessEJBLocator<AppClientSingletonRemote> locator = new StatelessEJBLocator(AppClientSingletonRemote.class,
                APP_NAME, MODULE_NAME, AppClientStateSingleton.class.getSimpleName(), "");
        final AppClientSingletonRemote remote = EJBClient.createProxy(locator);
        remote.reset();
        URL props = getClass().getClassLoader().getResource("jboss-ejb-client.properties");
        final AppClientWrapper wrapper = new AppClientWrapper(getArchive(),
                " -Dnode0=" + managementClient.getMgmtAddress() + " --ejb-client-properties=" + props, "client-override.jar",
                "");
        try {
            final String result = remote.awaitAppClientCall();
            assertTrue("App client call failed. App client output: " + wrapper.readAllUnformated(1000), result != null);
            assertEquals("OverridenEnvEntry", result);
        } finally {
            wrapper.quit();
        }
    }
}
