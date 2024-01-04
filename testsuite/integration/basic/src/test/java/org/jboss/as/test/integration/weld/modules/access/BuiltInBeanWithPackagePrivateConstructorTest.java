/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.modules.access;

import java.io.File;
import java.net.URL;

import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.module.util.TestModule;
import org.jboss.as.test.shared.GlowUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class BuiltInBeanWithPackagePrivateConstructorTest {

    @Inject
    private InjectedBean injectedBean;
    private static TestModule testModule;

    public static void doSetup() throws Exception {
        tearDown();

        URL url = BuiltInBeanWithPackagePrivateConstructor.class.getResource("test-module.xml");
        File moduleXmlFile = new File(url.toURI());
        testModule = new TestModule("test.module-accessibility", moduleXmlFile);
        JavaArchive jar = testModule.addResource("module-accessibility.jar");
        jar.addClass(BuiltInBeanWithPackagePrivateConstructor.class);
        jar.addAsManifestResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml");
        testModule.create(true);

    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (testModule != null) {
            testModule.remove();
        }
    }

    @Deployment
    public static Archive<?> getDeployment() throws Exception {
        // No actual setup when scanning the deployment prior to test execution.
        if (!GlowUtil.isGlowScan()) {
            doSetup();
        }
        return ShrinkWrap.create(WebArchive.class).addClasses(InjectedBean.class, BuiltInBeanWithPackagePrivateConstructorTest.class, GlowUtil.class)
                .addClass(TestModule.class)
                .addAsWebInfResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml")
                .addAsManifestResource(new StringAsset("Dependencies: test.module-accessibility meta-inf\n"), "MANIFEST.MF");

    }

    @Test
    public void testBeanInjectable() throws IllegalArgumentException, IllegalAccessException {
        BuiltInBeanWithPackagePrivateConstructor bean = injectedBean.getBean();
        bean.setValue("foo");
        Assert.assertEquals("foo", bean.getValue());
    }
}
