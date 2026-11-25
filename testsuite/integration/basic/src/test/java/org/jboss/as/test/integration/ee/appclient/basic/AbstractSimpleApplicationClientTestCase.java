/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.appclient.basic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.ee.appclient.util.AppClientWrapper;
import org.jboss.as.test.integration.jpa.packaging.Employee;
import org.jboss.as.test.integration.jpa.packaging.PersistenceUnitPackagingTestCase;
import org.jboss.as.test.shared.integration.ejb.security.CallbackHandler;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.StatelessEJBLocator;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

public abstract class AbstractSimpleApplicationClientTestCase {



    protected static final String MODULE_NAME = "ejb";

    public static EnterpriseArchive buildAppclientEar(String appName, boolean useCommonEjbInterface) {

        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, appName + ".ear");

        final JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "lib.jar");
        lib.addClasses(AppClientWrapper.class, CallbackHandler.class);
        if (useCommonEjbInterface) {
            lib.addClasses(AppClientSingletonRemote.class);
        }
        lib.addClasses(Employee.class);
        lib.addAsManifestResource(PersistenceUnitPackagingTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        ear.addAsLibrary(lib);

        final JavaArchive otherLib = ShrinkWrap.create(JavaArchive.class, "otherlib.jar");
        otherLib.addClass(Status.class);
        ear.addAsLibrary(otherLib);

        final JavaArchive ejb = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        ejb.addClasses(SimpleApplicationClientTestCase.class, AppClientStateSingleton.class);
        if (!useCommonEjbInterface) {
            ejb.addClasses(AppClientSingletonRemote.class);
        }
        ear.addAsModule(ejb);

        final JavaArchive appClient = ShrinkWrap.create(JavaArchive.class, "client-annotation.jar");
        appClient.addClasses(org.junit.Assert.class, org.junit.ComparisonFailure.class);
        appClient.addClasses(AppClientMain.class);
        if (!useCommonEjbInterface) {
            appClient.addClasses(AppClientSingletonRemote.class);
        }
        appClient.addAsManifestResource(new StringAsset("Main-Class: " + AppClientMain.class.getName() + "\n"), "MANIFEST.MF");
        ear.addAsModule(appClient);

        return ear;
    }

    @ArquillianResource
    protected ManagementClient managementClient;

    private final String appName;

    protected AbstractSimpleApplicationClientTestCase(String appName) {
        this.appName = appName;
    }

    public abstract Archive<?> getArchive();

    /**
     * Tests a simple app client that calls an ejb with its command line parameters
     */
    public void simpleAppClientTest() throws Exception {
        testAppClient(null, "client-annotation.jar", "${test.expr.applcient.param:cmdLineParam}", "cmdLineParam");
    }

    /**
     * Tests an app client with a deployment descriptor, that injects an env-entry and an EJB.
     *
     * @throws Exception
     */
    public void descriptorBasedAppClientTest() throws Exception {
        testAppClient(null, "client-dd.jar", null, "EnvEntry");
    }

    /**
     * Tests an app client with a deployment descriptor, that injects an env-entry and an EJB.
     *
     * @throws Exception
     */
    public void testAppClientJBossDescriptor() throws Exception {
        URL props = getClass().getClassLoader().getResource("jboss-ejb-client.properties");
        testAppClient(" -Dnode0=" + managementClient.getMgmtAddress() + getEjbClientPropertiesArgument(props), "client-override.jar", null, "OverridenEnvEntry");
    }

    protected void testAppClient(String hostArgument,  String deploymentName, String appclientArgs, String expectedResult) throws Exception {
        final StatelessEJBLocator<AppClientSingletonRemote> locator = new StatelessEJBLocator(AppClientSingletonRemote.class,
                appName, MODULE_NAME, AppClientStateSingleton.class.getSimpleName(), "");
        final AppClientSingletonRemote remote = EJBClient.createProxy(locator);
        remote.reset();
        final AppClientWrapper wrapper = new AppClientWrapper(getArchive(),
                hostArgument == null ? getHostArgument() : hostArgument,
                deploymentName,
                appclientArgs == null ? "" : appclientArgs);
        try {
            final String result = remote.awaitAppClientCall();
            assertTrue("App client call failed. App client output: " + wrapper.readAllUnformated(1000), result != null);
            assertEquals(expectedResult, result);
        } finally {
            wrapper.quit();
        }

    }

    private String getHostArgument() {
        // Use an expression for the host arg value to validate that works
        return "--host=${test.expr.appclient.host:" + managementClient.getRemoteEjbURL() + "}";
    }

    private String getEjbClientPropertiesArgument(URL props) {
        // Use an expression for the  arg value to validate that works
        return " --ejb-client-properties=${test.expr.appclient.properties:" + props + "}";
    }
}
