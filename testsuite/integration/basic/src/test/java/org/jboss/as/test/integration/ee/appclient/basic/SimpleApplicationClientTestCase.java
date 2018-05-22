/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.ee.appclient.util.AppClientWrapper;
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
