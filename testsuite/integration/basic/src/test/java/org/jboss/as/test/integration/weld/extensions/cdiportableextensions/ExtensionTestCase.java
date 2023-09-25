/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.weld.extensions.cdiportableextensions;

import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;

import jakarta.enterprise.inject.spi.Extension;
import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class ExtensionTestCase extends AbstractModuleTest {

    protected static String modulePath = "cidExtensionModule";

    @AfterClass
    public static void tearDown() throws Exception {
        doCleanup(modulePath);
    }

    protected static void doSetup() throws Exception {
        URL url = ExtensionTestCase.class.getResource("module.xml");
        if (url == null) {
            throw new IllegalStateException("Could not find module.xml");
        }

        JavaArchive moduleJar = ShrinkWrap.create(JavaArchive.class, "weldTest.jar");
        moduleJar.addClasses(FunExtension.class, Funny.class);
        moduleJar.addAsServiceProvider(Extension.class, FunExtension.class);

        doSetup(modulePath, url.openStream(), moduleJar);
    }

    @Deployment
    public static Archive<?> deploy() throws Exception {

        doSetup();

        JavaArchive jar = ShrinkWrap
                .create(JavaArchive.class, "test.jar")
                .addClasses(Clown.class, ExtensionTestCase.class, AbstractModuleTest.class)
                .addAsManifestResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml")
                .addAsManifestResource(new StringAsset("Dependencies: cidExtensionModule services\n"),
                        "MANIFEST.MF");

        return jar;
    }

    @Inject
    FunExtension funExtension;

    @Test
    public void testFoo() throws MalformedURLException {
        assertEquals("There should be one funny bean.", 1, funExtension.getFunnyBeans().size());
        assertEquals("Clown should be the funny bean.", Clown.class, funExtension.getFunnyBeans().iterator().next()
                .getBeanClass());
    }
}
