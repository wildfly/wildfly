/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.arquillian.container;

import java.util.jar.Manifest;

import org.jboss.arquillian.container.osgi.AbstractOSGiApplicationArchiveProcessor;
import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;


/**
 * An OSGi {@link ApplicationArchiveProcessor} that does not generate the manifest on demand.
 * AS7 test archives must be explicit about their manifest metadata.
 *
 * @author Thomas.Diesler@jboss.com
 */
public class OSGiApplicationArchiveProcessor implements ApplicationArchiveProcessor
{
    @Override
    public void process(Archive<?> appArchive, TestClass testClass) {
        if(isValidOSGiBundleArchive(appArchive)) {
            ApplicationArchiveProcessor processor = new AbstractOSGiApplicationArchiveProcessor() {
                @Override
                protected Manifest createBundleManifest(String symbolicName) {
                    return null;
                }
            };
            processor.process(appArchive, testClass);
        }
    }

    public static boolean isValidOSGiBundleArchive(Archive<?> appArchive) {
        // org.jboss.arquillian.container:arquillian-container-osgi must be be provided
        ClassLoader classLoader = OSGiApplicationArchiveProcessor.class.getClassLoader();
        try {
            classLoader.loadClass("org.jboss.arquillian.container.osgi.AbstractOSGiApplicationArchiveProcessor");
        } catch (ClassNotFoundException ex) {
            return false;
        }
        Manifest manifest = ManifestUtils.getManifest(appArchive);
        return OSGiManifestBuilder.isValidBundleManifest(manifest);
    }
}
