/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.test.integration.weld.configuration.requirebeandescriptor;

import javax.enterprise.inject.spi.BeanManager;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
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
        Assert.assertEquals(1, manager.getBeans(Foo.class).size());
        Assert.assertEquals(Foo.class, manager.getBeans(Foo.class).iterator().next().getBeanClass());
        Assert.assertTrue(manager.getBeans(Bar.class).isEmpty());
    }
}
