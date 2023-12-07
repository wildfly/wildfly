/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.modules.deployment;

import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.module.util.TestModule;
import org.jboss.as.test.shared.GlowUtil;
import org.jboss.as.test.shared.ModuleUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Verifies that a bean (FooImpl3) in the top-level deployment unit is visible from a static Jakarta Contexts and Dependency Injection enabled module.
 * At the same time beans from sub-deployments (FooImpl1, FooImpl2) are not visible.
 *
 * @author Jozef Hartinger
 *
 */
@RunWith(Arquillian.class)
public class StaticModuleToDeploymentVisibilityEarTest {

    private static final String MODULE_NAME = "weld-modules-deployment-ear";
    private static TestModule testModule;

    public static void doSetup() throws Exception {
        testModule = ModuleUtils.createTestModuleWithEEDependencies(MODULE_NAME);
        testModule.addResource("test-module.jar")
            .addClasses(ModuleBean.class, Foo.class)
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        testModule.create();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        testModule.remove();
    }

    @Deployment
    public static Archive<?> getDeployment() throws Exception {
        // No actual setup when scanning the deployment prior to test execution.
        if (!GlowUtil.isGlowScan()) {
            doSetup();
        }
        WebArchive war1 = ShrinkWrap.create(WebArchive.class)
                .addClasses(StaticModuleToDeploymentVisibilityEarTest.class, FooImpl1.class, TestModule.class, GlowUtil.class)
                .addAsWebInfResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml");
        WebArchive war2 = ShrinkWrap.create(WebArchive.class)
                .addClasses(FooImpl2.class)
                .addAsWebInfResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml");
        JavaArchive library = ShrinkWrap.create(JavaArchive.class)
                .addClass(FooImpl3.class)
                .addAsManifestResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml");
        return ShrinkWrap.create(EnterpriseArchive.class)
                .addAsModules(war1, war2)
                .addAsLibrary(library)
                .addAsManifestResource(new StringAsset("Dependencies: test." + MODULE_NAME + " meta-inf\n"), "MANIFEST.MF");
    }

    @Inject
    private ModuleBean alpha;

    @Test
    public void testBeanAccessibility() {
        Assert.assertNotNull(alpha.getFoo());
        Assert.assertTrue(alpha.getFoo() instanceof FooImpl3);
    }
}
