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
public class SimpleApplicationClientTestCase extends AbstractSimpleApplicationClientTestCase {

    private static Archive archive;

    @Override
    public Archive<?> getArchive() {
        return SimpleApplicationClientTestCase.archive;
    }

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear");

        final JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "lib.jar");
        lib.addClasses(AppClientSingletonRemote.class, AppClientWrapper.class, CallbackHandler.class);
        lib.addClasses(Employee.class);
        lib.addAsManifestResource(PersistenceUnitPackagingTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        ear.addAsLibrary(lib);

        final JavaArchive otherLib = ShrinkWrap.create(JavaArchive.class, "otherlib.jar");
        otherLib.addClass(Status.class);
        ear.addAsLibrary(otherLib);

        final JavaArchive ejb = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        ejb.addClasses(SimpleApplicationClientTestCase.class, AppClientStateSingleton.class);
        ear.addAsModule(ejb);

        final JavaArchive appClient = ShrinkWrap.create(JavaArchive.class, "client-annotation.jar");
        appClient.addClasses(org.junit.Assert.class, org.junit.ComparisonFailure.class);
        appClient.addClasses(AppClientMain.class);
        appClient.addAsManifestResource(new StringAsset("Main-Class: " + AppClientMain.class.getName() + "\n"), "MANIFEST.MF");
        ear.addAsModule(appClient);

        final JavaArchive clientDD = ShrinkWrap.create(JavaArchive.class, "client-dd.jar");
        clientDD.addClasses(DescriptorClientMain.class, org.junit.ComparisonFailure.class);
        clientDD.addClasses(org.junit.Assert.class);
        clientDD.addAsManifestResource(new StringAsset("Main-Class: " + DescriptorClientMain.class.getName() + "\n"), "MANIFEST.MF");
        clientDD.addAsManifestResource(SimpleApplicationClientTestCase.class.getPackage(), "application-client.xml", "application-client.xml");
        ear.addAsModule(clientDD);

        final JavaArchive clientOverride = ShrinkWrap.create(JavaArchive.class, "client-override.jar");
        clientOverride.addClasses(DescriptorClientMain.class, org.junit.ComparisonFailure.class);
        clientOverride.addClasses(org.junit.Assert.class);
        clientOverride.addAsManifestResource(new StringAsset("Main-Class: " + DescriptorClientMain.class.getName() + "\n"), "MANIFEST.MF");
        clientOverride.addAsManifestResource(SimpleApplicationClientTestCase.class.getPackage(), "application-client.xml", "application-client.xml");
        clientOverride.addAsManifestResource(SimpleApplicationClientTestCase.class.getPackage(), "jboss-client.xml", "jboss-client.xml");
        ear.addAsModule(clientOverride);

        archive = ear;
        return ear;
    }

    @Test
    public void simpleAppClientTest() throws Exception {
        super.simpleAppClientTest();
    }

    @Test
    public void descriptorBasedAppClientTest() throws Exception {
        super.descriptorBasedAppClientTest();
    }

    @Test
    public void testAppClientJBossDescriptor() throws Exception {
        super.testAppClientJBossDescriptor();
    }
}
