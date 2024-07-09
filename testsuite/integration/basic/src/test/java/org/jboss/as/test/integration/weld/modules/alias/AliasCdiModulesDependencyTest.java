/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.modules.alias;

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

import jakarta.inject.Inject;
import java.io.File;

/**
 *
 * @author <a href="mailto:bspyrkos@redhat.com">Bartosz Spyrko-Smietanko</a>
 */
@RunWith(Arquillian.class)
public class AliasCdiModulesDependencyTest {

    private static final String REF_MODULE_NAME = "weld-module-ref";
    private static final String IMPL_MODULE_NAME = "weld-module-impl";
    private static final String ALIAS_MODULE_NAME = "weld-module-alias";
    private static TestModule testModuleRef;
    private static TestModule testModuleImpl;
    private static TestModule testModuleAlias;

    public static void doSetup() throws Exception {
        testModuleImpl = ModuleUtils.createTestModuleWithEEDependencies(IMPL_MODULE_NAME);
        testModuleImpl.addResource("test-module.jar")
            .addClasses(ModuleBean.class)
            .addAsManifestResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml");
        testModuleImpl.create();

        // load the module.xml file and use it to create module, we need this to have Module B as exported dependency
        File moduleFile = new File(AliasCdiModulesDependencyTest.class.getResource("module-ref.xml").toURI());
        testModuleRef = new TestModule("test." + REF_MODULE_NAME, moduleFile);
        testModuleRef.create();

        File aliasModuleFile = new File(AliasCdiModulesDependencyTest.class.getResource("module-alias.xml").toURI());
        testModuleAlias = new TestModule("test." + ALIAS_MODULE_NAME, aliasModuleFile);
        testModuleAlias.create();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        testModuleAlias.remove();
        testModuleRef.remove();
        testModuleImpl.remove();
    }

    @Deployment
    public static WebArchive getDeployment() throws Exception {
        // No actual setup when scanning the deployment prior to test execution.
        if (!GlowUtil.isGlowScan()) {
            doSetup();
        }
        return ShrinkWrap.create(WebArchive.class)
            .addClasses(AliasCdiModulesDependencyTest.class, WarBean.class, TestModule.class, GlowUtil.class)
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            .addAsManifestResource(new StringAsset("Dependencies: test." + ALIAS_MODULE_NAME + " meta-inf export\n"), "MANIFEST.MF");
    }

    @Inject
    WarBean warBean;

    @Test
    public void testBeanAccessibilities() {
        Assert.assertNotNull(warBean.getInjectedBean());
    }
}
