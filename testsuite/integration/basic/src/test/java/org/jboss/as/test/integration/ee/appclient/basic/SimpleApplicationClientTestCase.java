/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.appclient.basic;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
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

    private static final String APP_NAME = SimpleApplicationClientTestCase.class.getSimpleName();
    private static EnterpriseArchive archive;

    public SimpleApplicationClientTestCase() {
        super(APP_NAME);
    }

    @Override
    public Archive<?> getArchive() {
        return SimpleApplicationClientTestCase.archive;
    }

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        final EnterpriseArchive ear = buildAppclientEar(APP_NAME, true);

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
