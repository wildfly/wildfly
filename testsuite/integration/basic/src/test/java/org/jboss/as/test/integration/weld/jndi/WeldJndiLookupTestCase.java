/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.weld.jndi;

import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
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
        jar.add(EmptyAsset.INSTANCE, "META-INF/beans.xml");
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
