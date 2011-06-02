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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.logging.Logger;
import org.jboss.osgi.spi.util.BundleInfo;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.impl.base.io.IOUtil;

/**
 * An {@link ApplicationArchiveProcessor} that delegates to deployment type specific packagers.
 *
 * @author Thomas.Diesler@jboss.com
 * @author Kabir Khan
 * @since 17-Nov-2010
 */
public class DelegatingApplicationArchiveProcessor implements ApplicationArchiveProcessor {

    private static final Logger log = Logger.getLogger(DelegatingApplicationArchiveProcessor.class);

    @Override
    public void process(Archive<?> appArchive, TestClass testClass) {
        ApplicationArchiveProcessor archiveProcessor;
        if (isBundleArchive(testClass, appArchive)) {
            archiveProcessor = new OSGiApplicationArchiveProcessor();
        } else {
            archiveProcessor = new ModuleApplicationArchiveProcessor();
        }

        log.debugf("Process archive '%s' with: %s", appArchive.getName(), archiveProcessor);
        archiveProcessor.process(appArchive, testClass);

        // Debug the application archive manifest
        ArchivePath manifestPath = ArchivePaths.create(JarFile.MANIFEST_NAME);
        Node node = appArchive.get(manifestPath);
        if (node == null) {
            log.errorf("Cannot find manifest in: %s", appArchive.getName());
        } else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                IOUtil.copy(node.getAsset().openStream(), baos);
            } catch (IOException ex) {
            }
            log.debugf("Manifest for %s: \n%s", appArchive.getName(), new String(baos.toByteArray()));
        }
    }

    private boolean isBundleArchive(TestClass testClass, Archive<?> appArchive) {
        Manifest manifest = ManifestUtils.getOrCreateManifest(appArchive);
        return BundleInfo.isValidBundleManifest(manifest);
    }
}
