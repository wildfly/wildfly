/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.weld.modules.deployment;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.hamcrest.CoreMatchers;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.module.util.TestModule;
import org.jboss.as.test.shared.ModuleUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.beans11.BeansDescriptor;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Verifies that the bean discovery mode defined in dependencies is taken into account.
 *
 * @author Yoann Rodiere
 *
 */
@RunWith(Arquillian.class)
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

    @AfterClass
    public static void tearDown() throws Exception {
        testModule.remove();
    }

    @Deployment
    public static Archive<?> getDeployment() throws Exception {
        doSetup();
        return ShrinkWrap.create(WebArchive.class)
                .addClasses(ModuleBeanDiscoveryModeTest.class, TestModule.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(new StringAsset("Dependencies: test." + MODULE_NAME + " meta-inf\n"), "MANIFEST.MF");
    }

    @Inject
    private Instance<Foo> injected;

    @Test
    public void testBeanDiscovery() {
        Assert.assertFalse(injected.isUnsatisfied());
        Assert.assertFalse(injected.isAmbiguous());
        Assert.assertThat(injected.get(), CoreMatchers.instanceOf(FooImplAnnotated.class));
    }
}
