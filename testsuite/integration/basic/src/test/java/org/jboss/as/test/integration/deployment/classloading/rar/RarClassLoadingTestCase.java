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
package org.jboss.as.test.integration.deployment.classloading.rar;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.jca.beanvalidation.ra.ValidConnectionFactory;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that nesteled jars in a rar are picked up as resource roots and loaded from the same class loader.
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class RarClassLoadingTestCase {

    @Deployment
    public static Archive<?> deployment() {
        ResourceAdapterArchive rar = ShrinkWrap.create(ResourceAdapterArchive.class, "rarClassLoadingTest.rar");
        JavaArchive jar1 = ShrinkWrap.create(JavaArchive.class, "main.jar");
        jar1.addClasses(RarClassLoadingTestCase.class, RarMainClass.class);
        jar1.addPackage(ValidConnectionFactory.class.getPackage());
        rar.add(jar1, "/", ZipExporter.class);
        JavaArchive jar2 = ShrinkWrap.create(JavaArchive.class, "support.jar");
        jar2.addClasses(RarSupportClass.class);
        rar.add(jar2, "some/random/directory", ZipExporter.class);
        rar.addAsManifestResource(RarClassLoadingTestCase.class.getPackage(), "ra.xml", "ra.xml");

        return rar;
    }

    @Test
    public void testRarClassLoading() {
        RarMainClass cl = new RarMainClass();
        RarSupportClass cl2 = new RarSupportClass();
        Assert.assertEquals(cl.getClass().getClassLoader(), cl2.getClass().getClassLoader());
    }

}
