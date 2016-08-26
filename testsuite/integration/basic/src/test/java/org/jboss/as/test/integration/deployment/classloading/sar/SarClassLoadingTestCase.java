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
package org.jboss.as.test.integration.deployment.classloading.sar;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that nested jars in a sar are picked up as resource roots and loaded from the same class loader.
 *
 * @author Tomasz Adamski
 */
@RunWith(Arquillian.class)
public class SarClassLoadingTestCase {

    @Deployment
    public static Archive<?> deployment() {
        JavaArchive sar = ShrinkWrap.create(JavaArchive.class, "sarClassLoadingTest.sar");
        sar.addClasses(SarClassLoadingTestCase.class, SarMainClass.class);
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "inner.jar");
        jar.addClasses(SarSupportClass.class);
        sar.add(jar, "/", ZipExporter.class);
        return sar;
    }

    @Test
    public void testSarClassLoading() {
        SarMainClass cl = new SarMainClass();
        SarSupportClass cl2 = new SarSupportClass();
        Assert.assertEquals(cl.getClass().getClassLoader(), cl2.getClass().getClassLoader());
    }

}