/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.arquillian.container;

import java.util.jar.Manifest;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.protocol.osgi.OSGiApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.osgi.spi.util.BundleInfo;
import org.jboss.shrinkwrap.api.Archive;

/**
 * An {@link ApplicationArchiveProcessor} that delegates to deployment type specific packagers.
 *
 * @author Thomas.Diesler@jboss.com
 * @author Kabir Khan
 * @since 17-Nov-2010
 */
public class DelegatingApplicationArchiveProcessor implements ApplicationArchiveProcessor {

    @Override
    public void process(Archive<?> appArchive, TestClass testClass) {
        ApplicationArchiveProcessor archiveProcessor;
        if (isBundleArchive(testClass, appArchive))
            archiveProcessor = new OSGiApplicationArchiveProcessor();
        else
            archiveProcessor = new ModuleApplicationArchiveProcessor();

        archiveProcessor.process(appArchive, testClass);
    }

    private boolean isBundleArchive(TestClass testClass, Archive<?> appArchive) {
        // Check if the archive contains a valid OSGi manifest
        Manifest manifest = ManifestUtils.getOrCreateManifest(appArchive);
        return BundleInfo.isValidateBundleManifest(manifest);
    }
}
