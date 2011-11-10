/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.ee.appclient.basic;

import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.ee.appclient.util.AppClientWrapper;
import org.jboss.as.test.integration.ejb.remote.common.EJBRemoteManagementUtil;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.EJBClientTransactionContext;
import org.jboss.ejb.client.StatelessEJBLocator;
import org.jboss.ejb.client.remoting.IoFutureHelper;
import org.jboss.remoting3.Connection;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.Remoting;
import org.jboss.remoting3.remote.RemoteConnectionProviderFactory;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xnio.IoFuture;
import org.xnio.OptionMap;
import org.xnio.Options;

import static org.jboss.as.arquillian.container.Authentication.getCallbackHandler;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests that an application client can launch and conntect to a remote EJB
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SimpleApplicationClientTestCase {


    private static Connection connection;

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    private EJBClientContext ejbClientContext;

    private static Archive archive;


    private static final String APP_NAME = "simple-app-client-test";

    private static final String MODULE_NAME = "ejb";

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear");

        final JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "lib.jar");
        lib.addClasses(AppClientSingletonRemote.class, AppClientWrapper.class);
        ear.addAsLibrary(lib);

        final JavaArchive ejb = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        ejb.addClasses(SimpleApplicationClientTestCase.class, AppClientStateSingleton.class);
        ear.addAsModule(ejb);


        final JavaArchive appClient = ShrinkWrap.create(JavaArchive.class, "client-annotation.jar");
        appClient.addClasses(AppClientMain.class);
        appClient.addAsManifestResource(new StringAsset("Main-Class: " + AppClientMain.class.getName() + "\n"), "MANIFEST.MF");
        ear.addAsModule(appClient);


        final JavaArchive clientDD = ShrinkWrap.create(JavaArchive.class, "client-dd.jar");
        clientDD.addClasses(DescriptorClientMain.class);
        clientDD.addAsManifestResource(new StringAsset("Main-Class: " + DescriptorClientMain.class.getName() + "\n"), "MANIFEST.MF");
        clientDD.addAsManifestResource(SimpleApplicationClientTestCase.class.getPackage(), "application-client.xml", "application-client.xml");
        ear.addAsModule(clientDD);

        final JavaArchive clientOverride = ShrinkWrap.create(JavaArchive.class, "client-override.jar");
        clientOverride.addClasses(DescriptorClientMain.class);
        clientOverride.addAsManifestResource(new StringAsset("Main-Class: " + DescriptorClientMain.class.getName() + "\n"), "MANIFEST.MF");
        clientOverride.addAsManifestResource(SimpleApplicationClientTestCase.class.getPackage(), "application-client.xml", "application-client.xml");
        clientOverride.addAsManifestResource(SimpleApplicationClientTestCase.class.getPackage(), "jboss-client.xml", "jboss-client.xml");
        ear.addAsModule(clientOverride);

        archive = ear;
        return ear;
    }

    /**
     * Create and setup the remoting connection
     *
     * @throws Exception
     */
    @BeforeClass
    public static void beforeTestClass() throws Exception {
        final Endpoint endpoint = Remoting.createEndpoint("ejb-remote-client-endpoint", OptionMap.EMPTY);
        endpoint.addConnectionProvider("remote", new RemoteConnectionProviderFactory(), OptionMap.create(Options.SSL_ENABLED, Boolean.FALSE));


        // open a connection
        final int ejbRemotingPort = EJBRemoteManagementUtil.getEJBRemoteConnectorPort("localhost", 9999, getCallbackHandler());
        final IoFuture<Connection> futureConnection = endpoint.connect(new URI("remote://localhost:" + ejbRemotingPort), OptionMap.create(Options.SASL_POLICY_NOANONYMOUS, Boolean.FALSE), getCallbackHandler());
        connection = IoFutureHelper.get(futureConnection, 5, TimeUnit.SECONDS);
    }

    @AfterClass
    public static void afterTestClass() throws Exception {
        executor.shutdown();
    }

    /**
     * Create and setup the EJB client context backed by the remoting receiver
     *
     * @throws Exception
     */
    @Before
    public void beforeTest() throws Exception {
        this.ejbClientContext = EJBClientContext.create();
        this.ejbClientContext.registerConnection(connection);
        final EJBClientTransactionContext localUserTxContext = EJBClientTransactionContext.createLocal();
        // set the tx context
        EJBClientTransactionContext.setGlobalContext(localUserTxContext);

    }

    @After
    public void afterTest() throws Exception {
        if (this.ejbClientContext != null) {
            EJBClientContext.suspendCurrent();
        }
    }

    /**
     * Tests a simple app client that calls an ejb with its command line parameters
     */
    @Test
    public void simpleAppClientTest() throws Exception {
        final StatelessEJBLocator<AppClientSingletonRemote> locator = new StatelessEJBLocator(AppClientSingletonRemote.class, APP_NAME, MODULE_NAME, AppClientStateSingleton.class.getSimpleName(), "");
        final AppClientSingletonRemote remote = EJBClient.createProxy(locator);
        remote.reset();
        final AppClientWrapper wrapper = new AppClientWrapper(archive, "client-annotation.jar", "cmdLineParam");
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
     * @throws Exception
     */
    @Test
    public void descriptorBasedAppClientTest() throws Exception {
        final StatelessEJBLocator<AppClientSingletonRemote> locator = new StatelessEJBLocator(AppClientSingletonRemote.class, APP_NAME, MODULE_NAME, AppClientStateSingleton.class.getSimpleName(), "");
        final AppClientSingletonRemote remote = EJBClient.createProxy(locator);
        remote.reset();
        final AppClientWrapper wrapper = new AppClientWrapper(archive, "client-dd.jar", "");
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
     * @throws Exception
     */
    @Test
    @Ignore("This needs jboss-client.xml parsing")
    public void testAppClientJBossDescriptor() throws Exception {
        final StatelessEJBLocator<AppClientSingletonRemote> locator = new StatelessEJBLocator(AppClientSingletonRemote.class, APP_NAME, MODULE_NAME, AppClientStateSingleton.class.getSimpleName(), "");
        final AppClientSingletonRemote remote = EJBClient.createProxy(locator);
        remote.reset();
        final AppClientWrapper wrapper = new AppClientWrapper(archive, "client-override.jar", "");
        try {
            final String result = remote.awaitAppClientCall();
            assertTrue("App client call failed. App client output: " + wrapper.readAllUnformated(1000), result != null);
            assertEquals("OverridenEnvEntry", result);
        } finally {
            wrapper.quit();
        }
    }

}
