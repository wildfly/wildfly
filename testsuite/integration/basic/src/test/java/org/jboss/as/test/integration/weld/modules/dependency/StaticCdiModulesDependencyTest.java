/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.modules.dependency;

import java.io.File;

import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.module.util.TestModule;
import org.jboss.as.test.shared.GlowUtil;
import org.jboss.as.test.shared.ModuleUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author <a href="mailto:manovotn@redhat.com">Matej Novotny</a>
 */
@RunWith(Arquillian.class)
public class StaticCdiModulesDependencyTest {

    private static final String MODULE_NAME_A = "weld-module-A";
    private static final String MODULE_NAME_B = "weld-module-B";
    private static TestModule testModuleA;
    private static TestModule testModuleB;

    public static void doSetup() throws Exception {
        testModuleB = ModuleUtils.createTestModuleWithEEDependencies(MODULE_NAME_B);
        testModuleB.addResource("test-module-b.jar")
            .addClasses(ModuleBBean.class)
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        testModuleB.create();

        // load the module.xml file and use it to create module, we need this to have Module B as exported dependency
        File moduleFile = new File(ModuleABean.class.getResource("moduleA.xml").toURI());
        testModuleA = new TestModule("test." + MODULE_NAME_A, moduleFile);
        testModuleA.addResource("test-module-a.jar")
            .addClasses(ModuleABean.class)
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        testModuleA.create();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        testModuleA.remove();
        testModuleB.remove();
    }

    @Deployment
    public static WebArchive getDeployment() throws Exception {
        // No actual setup when scanning the deployment prior to test execution.
        if (!GlowUtil.isGlowScan()) {
            // create modules and deploy them
            doSetup();
        }
        return ShrinkWrap.create(WebArchive.class)
            .addClasses(StaticCdiModulesDependencyTest.class, WarBean.class, TestModule.class)
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            .addAsManifestResource(new StringAsset("Dependencies: test." + MODULE_NAME_A + " meta-inf export\n"), "MANIFEST.MF");
    }

    @Inject
    WarBean warBean;

    @Test
    public void testBeanAccessibilities() {
        // test that WAR can use Module A bean
        Assert.assertEquals(ModuleABean.class.getSimpleName(), warBean.getModuleABean().ping());
        // verify that you can do WAR -> Module A -> Module B
        // this way we verify that module a can see and use beans from module B
        Assert.assertEquals(ModuleBBean.class.getSimpleName(), warBean.getModuleABean().pingModuleBBean());
    }
}
