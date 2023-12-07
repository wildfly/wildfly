/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.modules;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.module.util.TestModule;
import org.jboss.as.test.shared.GlowUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.inject.Inject;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


/**
 * Tests that multiple jars in a module are handled correctly
 *
 * see WFLY-3419
 *
 */
@RunWith(Arquillian.class)
public class MultipleJarInModuleTestCase {

    private static final List<TestModule> testModules = new ArrayList<>();

    public static void doSetup() throws Exception {
        URL url = MultipleJarInModuleTestCase.class.getResource("multiple-module.xml");
        File moduleXmlFile = new File(url.toURI());
        TestModule testModule = new TestModule("test.multiple", moduleXmlFile);
        JavaArchive jar = testModule.addResource("m1.jar");
        jar.addClass(Multiple1.class);
        jar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        jar = testModule.addResource("m2.jar");
        jar.addClass(Multiple2.class);
        jar.addAsManifestResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml");
        testModules.add(testModule);
        testModule.create(true);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        for (TestModule testModule : testModules) {
            testModule.remove();
        }
    }

    @Deployment
    public static Archive<?> getDeployment() throws Exception {
        // No actual setup when scanning the deployment prior to test execution.
        if (!GlowUtil.isGlowScan()) {
            doSetup();
        }
        WebArchive war = ShrinkWrap.create(WebArchive.class)
                .addClass(MultipleJarInModuleTestCase.class)
                .addClass(TestModule.class)
                .addClass(GlowUtil.class)
                .addAsWebInfResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml")
                .addAsManifestResource(new StringAsset("Dependencies: test.multiple meta-inf\n"), "MANIFEST.MF");
        return war;
    }

    @Inject
    private Multiple2 multiple2;

    @Test
    public void testMultipleJarsInModule() {
        Assert.assertEquals("hello", multiple2.getMessage());

    }
}
