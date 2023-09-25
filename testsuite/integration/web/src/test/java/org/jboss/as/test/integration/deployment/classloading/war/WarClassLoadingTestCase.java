/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.deployment.classloading.war;


import static org.junit.Assert.*;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jboss.as.test.integration.common.WebInfLibClass;

@RunWith(Arquillian.class)
public class WarClassLoadingTestCase {

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class);
        war.addClass(WarClassLoadingTestCase.class);
        war.add(EmptyAsset.INSTANCE, "META-INF/example.txt");
        war.add(EmptyAsset.INSTANCE, "example2.txt");
        JavaArchive libJar = ShrinkWrap.create(JavaArchive.class);
        libJar.addClass(WebInfLibClass.class);
        war.addAsLibraries(libJar);
        return war;
    }

    @Test
    public void testWebInfLibAccessible() throws ClassNotFoundException {
        loadClass("org.jboss.as.test.integration.common.WebInfLibClass");
    }

    /*
     * Test case for AS7-5172. Ensure META-INF/* is accessible
     */
    @Test
    public void testMetaInfAccessible() throws ClassNotFoundException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL manifestResource = cl.getResource("META-INF/example.txt");
        assertNotNull(manifestResource);
    }

    /*
     * Test case for AS7-5172. Ensure that non-META-INF/* is not accessible
     */
    @Test
    public void testNonMetaInfNotAccessible() throws ClassNotFoundException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL nonManifestResource = cl.getResource("example2.txt");
        assertNull(nonManifestResource);
    }

    private static Class<?> loadClass(String name) throws ClassNotFoundException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl != null) {
            try {
                return Class.forName(name, false, cl);
            } catch (ClassNotFoundException ex) {
                return Class.forName(name);
            }
        } else
            return Class.forName(name);
    }
}
