/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.jndi;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;

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
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class WeldJndiLookupTestCase {

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "WeldJndiLookupTestCase.jar");
        jar.addPackage(WeldJndiLookupTestCase.class.getPackage());
        jar.add(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "META-INF/beans.xml");
        jar.addAsManifestResource(WeldJndiLookupTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        return jar;
    }

    @Inject
    private StartupSingletonEjb ejb;

    @Inject
    private AppNameInjector appNameInjector;

    @Test
    public void testBeanManagerCanBeLookedUp() throws NamingException {
        BeanManager bm = (BeanManager) new InitialContext().lookup("java:comp/BeanManager");
        Assert.assertNotNull(bm);
    }

    @Test
    public void testOtherJNDIbindingsAreAvailableAtStartup() {
        Assert.assertEquals("WeldJndiLookupTestCase", ejb.getName());
    }

    @Test
    public void testBeanManagerResourceLookup() {
        Assert.assertTrue( ejb.getBeanManager() instanceof BeanManager);
    }
    @Test
    public void testCdiBeanWithResourceName() {
        Assert.assertEquals("foo-value", appNameInjector.getFoo());
    }
}
