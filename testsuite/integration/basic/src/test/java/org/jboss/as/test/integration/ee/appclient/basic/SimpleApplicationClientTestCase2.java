/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.appclient.basic;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.ee.appclient.util.AppClientWrapper;
import org.jboss.as.test.integration.jpa.packaging.Employee;
import org.jboss.as.test.integration.jpa.packaging.PersistenceUnitPackagingTestCase;
import org.jboss.as.test.shared.integration.ejb.security.CallbackHandler;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that an application client can launch and conntect to a remote EJB
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SimpleApplicationClientTestCase2 extends AbstractSimpleApplicationClientTestCase {

    private static final String APP_NAME = SimpleApplicationClientTestCase2.class.getSimpleName();
    private static EnterpriseArchive archive;

    public SimpleApplicationClientTestCase2() {
        super(APP_NAME);
    }

    @Override
    public Archive<?> getArchive() {
        return SimpleApplicationClientTestCase2.archive;
    }

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear");

        final JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "lib.jar");
        lib.addClasses(AppClientSingletonRemote.class, AppClientWrapper.class, CallbackHandler.class, ClientInterceptor.class);
        lib.addClasses(Employee.class);
        lib.addAsManifestResource(PersistenceUnitPackagingTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        ear.addAsLibrary(lib);

        final JavaArchive otherLib = ShrinkWrap.create(JavaArchive.class, "otherlib.jar");
        otherLib.addClass(Status.class);
        ear.addAsLibrary(otherLib);

        final JavaArchive ejb = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        ejb.addClasses(SimpleApplicationClientTestCase2.class, AppClientStateSingleton.class);
        ear.addAsModule(ejb);

        final JavaArchive appClient = ShrinkWrap.create(JavaArchive.class, "client-annotation.jar");
        appClient.addClasses(AppClientMain.class);
        appClient.addClasses(org.junit.Assert.class, org.junit.ComparisonFailure.class);
        appClient.addAsManifestResource(new StringAsset("Main-Class: " + AppClientMain.class.getName() + "\n"), "MANIFEST.MF");
        ear.addAsModule(appClient);

        final JavaArchive clientOverride = ShrinkWrap.create(JavaArchive.class, "client-override.jar");
        clientOverride.addClasses(org.junit.Assert.class, org.junit.ComparisonFailure.class);
        clientOverride.addClasses(DescriptorClientMain.class);
        clientOverride.addAsManifestResource(new StringAsset("Main-Class: " + DescriptorClientMain.class.getName() + "\n"),
                "MANIFEST.MF");
        clientOverride.addAsManifestResource(SimpleApplicationClientTestCase2.class.getPackage(), "application-client.xml",
                "application-client.xml");
        clientOverride.addAsManifestResource(SimpleApplicationClientTestCase2.class.getPackage(), "jboss-client.xml",
                "jboss-client.xml");
        clientOverride.addAsResource(new StringAsset(ClientInterceptor.class.getCanonicalName()),
                "META-INF/services/org.jboss.ejb.client.EJBClientInterceptor");

        ear.addAsModule(clientOverride);
        archive = ear;
        return ear;
    }

    @Test
    public void simpleAppClientTest() throws Exception {
        super.simpleAppClientTest();
    }

}
