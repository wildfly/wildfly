/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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