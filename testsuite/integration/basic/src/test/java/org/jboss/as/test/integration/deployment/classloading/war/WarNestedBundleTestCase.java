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
package org.jboss.as.test.integration.deployment.classloading.war;

import static org.junit.Assert.assertTrue;

import java.io.InputStream;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.osgi.spi.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * [AS7-945] Cannot deploy EAR/WAR with nested bundle
 *
 * @author thomas.diesler@jboss.com
 * @since 23-Aug-2011
 */
@RunWith(Arquillian.class)
public class WarNestedBundleTestCase {

    @Deployment
    public static Archive<?> deploy() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "as945.war");
        war.addClass(WarNestedBundleTestCase.class);
        final JavaArchive libJar = ShrinkWrap.create(JavaArchive.class, "nested-bundle.jar");
        libJar.addClass(WebInfLibClass.class);
        libJar.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(libJar.getName());
                builder.addBundleManifestVersion(2);
                return builder.openStream();
            }
        });
        war.addAsLibraries(libJar);
        return war;
    }

    @Test
    public void testWebInfLibAccessible() throws ClassNotFoundException {
        ClassLoader warLoader = WarNestedBundleTestCase.class.getClassLoader();
        Class<?> clazz = warLoader.loadClass("org.jboss.as.test.integration.deployment.classloading.war.WebInfLibClass");
        ClassLoader bundleLoader = clazz.getClassLoader();
        assertTrue(bundleLoader.toString().contains("ModuleClassLoader for Module \"deployment.as945.war:main\""));
    }
}
