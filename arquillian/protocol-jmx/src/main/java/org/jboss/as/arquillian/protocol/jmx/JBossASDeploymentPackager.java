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
package org.jboss.as.arquillian.protocol.jmx;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Manifest;

import org.jboss.arquillian.container.test.spi.TestDeployment;
import org.jboss.arquillian.container.test.spi.client.deployment.DeploymentPackager;
import org.jboss.arquillian.container.test.spi.client.deployment.ProtocolArchiveProcessor;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.protocol.jmx.JMXProtocol;
import org.jboss.as.arquillian.protocol.jmx.JBossASProtocol.ServiceArchiveHolder;
import org.jboss.as.arquillian.service.ArquillianService;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.osgi.spi.util.BundleInfo;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
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

    private ServiceArchiveHolder serviceArchive;

    JBossASDeploymentPackager(ServiceArchiveHolder serviceArchive) {
        this.serviceArchive = serviceArchive;
    }

    @Override
    public Archive<?> generateDeployment(TestDeployment testDeployment, Collection<ProtocolArchiveProcessor> protocolProcessors) {

        final Archive<?> appArchive = testDeployment.getApplicationArchive();
        final Collection<Archive<?>> auxArchives = testDeployment.getAuxiliaryArchives();
        generateArquillianServiceArchive(appArchive, auxArchives);

        final Manifest manifest = ManifestUtils.getOrCreateManifest(testDeployment.getApplicationArchive());
        if (BundleInfo.isValidateBundleManifest(manifest) == false) {
            // JBAS-9059 Inconvertible types error due to OpenJDK compiler bug
            // if (appArchive instanceof WebArchive) {
            if (WebArchive.class.isAssignableFrom(appArchive.getClass())) {
                final ArchivePath webInfLib = ArchivePaths.create("WEB-INF", "lib");
                for (Archive<?> aux : auxArchives) {
                    if (!excludedAuxillaryArchives.contains(aux.getName())) {
                        appArchive.add(aux, webInfLib, ZipExporter.class);
                    }
                }
            } else {
                for (Archive<?> aux : auxArchives) {
                    if (!excludedAuxillaryArchives.contains(aux.getName())) {
                        appArchive.merge(aux);
                    }
                }
            }
        }
        return appArchive;
    }

    private void generateArquillianServiceArchive(Archive<?> appArchive, Collection<Archive<?>> auxArchives) {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "arquillian-service");
        archive.addPackage(ArquillianService.class.getPackage());
        archive.addPackage(JMXProtocol.class.getPackage());

        // Merge the auxilliary archives and collect the loadable extensions
        final Set<String> loadableExtensions = new HashSet<String>();
        final String loadableExtentionsPath = "META-INF/services/" + LoadableExtension.class.getName();
        for (Archive<?> aux : auxArchives) {
            Node node = aux.get(loadableExtentionsPath);
            if (node != null) {
                BufferedReader br = new BufferedReader(new InputStreamReader(node.getAsset().openStream()));
                try {
                    String line = br.readLine();
                    while (line != null) {
                        loadableExtensions.add(line);
                        line = br.readLine();
                    }
                } catch (IOException ex) {
                    // ignore
                }
            }
            archive.merge(aux);
        }

        // Generate the manifest with it's dependencies
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                ManifestBuilder builder = ManifestBuilder.newInstance();
                StringBuffer dependencies = new StringBuffer();
                dependencies.append("org.jboss.as.jmx,");
                dependencies.append("org.jboss.as.server,");
                dependencies.append("org.jboss.as.osgi,");
                dependencies.append("org.jboss.jandex,");
                dependencies.append("org.jboss.logging,");
                dependencies.append("org.jboss.modules,");
                dependencies.append("org.jboss.msc,");
                dependencies.append("org.jboss.osgi.framework,");
                dependencies.append("org.osgi.core");
                builder.addManifestHeader("Dependencies", dependencies.toString());
                return builder.openStream();
            }
        });

        // Add the ServiceActivator
        String serviceActivatorPath = "META-INF/services/" + ServiceActivator.class.getName();
        archive.addAsResource("arquillian-service/" + serviceActivatorPath, serviceActivatorPath);

        // Replace the LoadableExtension with the collected set
        archive.delete(ArchivePaths.create(loadableExtentionsPath));
        archive.addAsResource(new Asset() {
            @Override
            public InputStream openStream() {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintWriter pw = new PrintWriter(new OutputStreamWriter(baos));
                for (String line : loadableExtensions) {
                    pw.println(line);
                }
                pw.close();
                return new ByteArrayInputStream(baos.toByteArray());
            }
        }, loadableExtentionsPath);

        // Make the service archive availbale to {@link ArquillianServiceDeployer}
        serviceArchive.setArchive(archive);
    }
}
