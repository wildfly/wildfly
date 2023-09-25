/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.constructor;

import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * Tests that Session beans can be instantiated using the bean constructor, rather than
 * the default constructor
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class EjbConstructorInjectionTestCase {

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class);
        jar.addPackage(EjbConstructorInjectionTestCase.class.getPackage());
        jar.addAsManifestResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml");
        return jar;
    }

    @Inject
    private Kennel bean;

    @Inject
    private NoDefaultCtorView noDefaultCtorView;

    @Test
    public void testSessionBeanConstructorInjection() {
        Assert.assertNotNull(bean.getDog());
    }

    @Test
    public void testSessionBeanConstructorInjectionWithDoDefaultCtor() {
        Assert.assertNotNull(noDefaultCtorView.getDog());
    }
}
