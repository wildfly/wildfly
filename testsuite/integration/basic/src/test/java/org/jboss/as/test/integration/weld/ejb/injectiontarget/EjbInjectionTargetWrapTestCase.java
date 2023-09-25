/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.injectiontarget;

import jakarta.enterprise.inject.spi.Extension;
import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * AS7-1373
 *
 * Tests that EJB's InjectionTarget can be wrapped.
 *
 * @author Jozef Hartinger
 */
@RunWith(Arquillian.class)
public class EjbInjectionTargetWrapTestCase {
    @Deployment
    public static JavaArchive getDeployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class);
        jar.addPackage(EjbInjectionTargetWrapTestCase.class.getPackage());
        jar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        jar.addAsServiceProvider(Extension.class, WrappingExtension.class);
        return jar;
    }

    @Inject
    private Bus bus;

    @Test
    public void testEjbInjectionTargetWasWrapped() {
        Assert.assertTrue(bus.isInitialized());
    }
}
