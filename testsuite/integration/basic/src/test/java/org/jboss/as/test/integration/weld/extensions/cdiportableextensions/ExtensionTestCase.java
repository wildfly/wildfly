/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.jboss.as.test.integration.weld.extensions.cdiportableextensions;

import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;

import javax.enterprise.inject.spi.Extension;
import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
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
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
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
