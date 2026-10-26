/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.configuration.requirebeandescriptor;

import jakarta.enterprise.inject.spi.BeanManager;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ArquillianExtension.class)
public class RequireBeanDescriptorTest {

    @Deployment
    public static Archive<?> getDeployment() {
        JavaArchive library = ShrinkWrap.create(JavaArchive.class).addClass(Bar.class);
        return ShrinkWrap.create(WebArchive.class).addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addClasses(Foo.class)
                .addAsManifestResource(RequireBeanDescriptorTest.class.getPackage(), "jboss-all.xml", "jboss-all.xml")
                .addAsLibrary(library);
    }

    @Test
    public void testImplicitBeanArchiveWithoutBeanDescriptorNotDiscovered(BeanManager manager) {
        Assertions.assertEquals(1, manager.getBeans(Foo.class).size());
        Assertions.assertEquals(Foo.class, manager.getBeans(Foo.class).iterator().next().getBeanClass());
        Assertions.assertTrue(manager.getBeans(Bar.class).isEmpty());
    }
}
