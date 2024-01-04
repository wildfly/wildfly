/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
