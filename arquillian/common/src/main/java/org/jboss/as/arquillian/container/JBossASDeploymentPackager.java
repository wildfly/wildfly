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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Manifest;

import org.jboss.arquillian.container.test.spi.TestDeployment;
import org.jboss.arquillian.container.test.spi.client.deployment.DeploymentPackager;
import org.jboss.arquillian.container.test.spi.client.deployment.ProtocolArchiveProcessor;
import org.jboss.osgi.spi.util.BundleInfo;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * A {@link DeploymentPackager} that for AS7 test deployments.
 *
 * @author Thomas.Diesler@jboss.com
 * @author Kabir Khan
 * @since 17-Nov-2010
 */
public class JBossASDeploymentPackager implements DeploymentPackager {

    private static final Set<String> excludedAuxillaryArchives = new HashSet<String>();

    static {
        excludedAuxillaryArchives.add("arquillian-core.jar");
        excludedAuxillaryArchives.add("arquillian-junit.jar");
    }

    @Override
    public Archive<?> generateDeployment(TestDeployment testDeployment, Collection<ProtocolArchiveProcessor> protocolProcessors) {

        final Manifest manifest = ManifestUtils.getOrCreateManifest(testDeployment.getApplicationArchive());
        final Archive<?> appArchive = testDeployment.getApplicationArchive();
        final Collection<Archive<?>> auxArchives = testDeployment.getAuxiliaryArchives();
        if (BundleInfo.isValidateBundleManifest(manifest)) {
            // Arquillian generates auxiliary archives that aren't bundles
            // auxArchives.clear();
            merge(appArchive, auxArchives);
        } else {
            // JBAS-9059 Inconvertible types error due to OpenJDK compiler bug
            //if (appArchive instanceof WebArchive) {
            if (WebArchive.class.isAssignableFrom(appArchive.getClass())) {
                final ArchivePath webInfLib = ArchivePaths.create("WEB-INF", "lib");
                for (Archive<?> aux : auxArchives) {
                    // we don't want to include the arquillian-core.jar and arquillian-junit.jar
                    // auxillary archives, as these are already part of the container
                    if (!excludedAuxillaryArchives.contains(aux.getName())) {
                        appArchive.add(aux, webInfLib, ZipExporter.class);
                    }
                }
            } else {
                merge(appArchive, auxArchives);
            }
        }
        return appArchive;
    }

    private void merge(Archive<?> appArchive, Collection<Archive<?>> auxArchives) {
        for (Archive<?> aux : auxArchives) {
            // we don't want to include the arquillian-core.jar and arquillian-junit.jar
            // auxillary archives, as these are already part of the container
            if (!excludedAuxillaryArchives.contains(aux.getName())) {
                appArchive.merge(aux);
            }
        }
    }
}
