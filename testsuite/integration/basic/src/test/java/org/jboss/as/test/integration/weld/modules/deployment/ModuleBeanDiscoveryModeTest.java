/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.modules.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.test.module.util.TestModule;
import org.jboss.as.test.shared.GlowUtil;
import org.jboss.as.test.shared.ModuleUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.beans11.BeansDescriptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Verifies that the bean discovery mode defined in dependencies is taken into account.
 *
 * @author Yoann Rodiere
 *
 */
@ExtendWith(ArquillianExtension.class)
public class ModuleBeanDiscoveryModeTest {

    private static final String MODULE_NAME = "module-with-annotated-bean-discovery-mode";
    private static TestModule testModule;

    public static void doSetup() throws Exception {
        testModule = ModuleUtils.createTestModuleWithEEDependencies(MODULE_NAME);
        testModule.addResource("test-module.jar")
            .addClass(Foo.class)
            /*
             * Annotated with @ApplicationScoped, should be added to the CDI context.
             */
            .addClass(FooImplAnnotated.class)
            /*
             * Not annotated, should be ignored as per the beans.xml
             * (so there should be no ambiguity when resolving implementations for Foo).
             */
            .addClass(FooImpl1.class)
            .addAsManifestResource(createBeansXml("annotated"), "beans.xml");
        testModule.create();

    }

    private static Asset createBeansXml(String beanDiscoveryMode) {
        String beansXml = Descriptors.create(BeansDescriptor.class)
                .version("1.1")
                .beanDiscoveryMode(beanDiscoveryMode)
                .exportAsString();
        return new StringAsset(beansXml);
    }

    @AfterAll
    public static void tearDown() throws Exception {
        testModule.remove();
    }

    @Deployment
    public static Archive<?> getDeployment() throws Exception {
        // No actual setup when scanning the deployment prior to test execution.
        if (!GlowUtil.isGlowScan()) {
            doSetup();
        }
        return ShrinkWrap.create(WebArchive.class)
                .addClasses(ModuleBeanDiscoveryModeTest.class, TestModule.class, GlowUtil.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(new StringAsset("Dependencies: test." + MODULE_NAME + " meta-inf\n"), "MANIFEST.MF");
    }

    @Inject
    private Instance<Foo> injected;

    @Test
    public void testBeanDiscovery() {
        Assertions.assertFalse(injected.isUnsatisfied());
        Assertions.assertFalse(injected.isAmbiguous());
        assertThat(injected.get()).isInstanceOf(FooImplAnnotated.class);
    }
}
