/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.deployment.classloading.sar;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests that nested jars in a sar are picked up as resource roots and loaded from the same class loader.
 *
 * @author Tomasz Adamski
 */
@ExtendWith(ArquillianExtension.class)
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
        Assertions.assertEquals(cl.getClass().getClassLoader(), cl2.getClass().getClassLoader());
    }

}