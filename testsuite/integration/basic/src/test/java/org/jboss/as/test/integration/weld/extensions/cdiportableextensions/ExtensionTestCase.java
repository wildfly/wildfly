/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.weld.extensions.cdiportableextensions;

import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;

import jakarta.enterprise.inject.spi.Extension;
import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.module.util.TestModule;
import org.jboss.as.test.shared.GlowUtil;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class ExtensionTestCase {

    private static final String MODULE_NAME = "cidExtensionModule";
    private static TestModule testModule;

    @AfterClass
    public static void tearDown() throws Exception {
        if (!AssumeTestGroupUtil.isBootableJar()) {
            testModule.remove();
        }
    }

    private static void doSetup() throws Exception {
        testModule = new TestModule(MODULE_NAME, "jakarta.annotation.api", "jakarta.enterprise.api");
        JavaArchive weldTestJar = testModule.addResource("weldTest.jar");
        weldTestJar.addClasses(FunExtension.class, Funny.class);
        weldTestJar.addAsServiceProvider(Extension.class, FunExtension.class);
        testModule.create();
    }

    @Deployment
    public static Archive<?> deploy() throws Exception {

        // No actual setup when scanning the deployment prior to test execution.
        // And skip for bootable jar, module is injected to bootable jar during provisioning, see pom.xml
        if (!GlowUtil.isGlowScan() && !AssumeTestGroupUtil.isBootableJar()) {
            doSetup();
        }

        JavaArchive jar = ShrinkWrap
                .create(JavaArchive.class, "test.jar")
                .addClasses(Clown.class, ExtensionTestCase.class, GlowUtil.class, TestModule.class)
                .addAsManifestResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml")
                .addAsManifestResource(new StringAsset("Dependencies: cidExtensionModule services\n"),
                        "MANIFEST.MF");

        return jar;
    }

    @Inject
    FunExtension funExtension;

    @Test
    public void testFoo() throws MalformedURLException {
        assertEquals("There should be one funny bean.", 1, funExtension.getFunnyBeans().size());
        assertEquals("Clown should be the funny bean.", Clown.class, funExtension.getFunnyBeans().iterator().next()
                .getBeanClass());
    }
}
