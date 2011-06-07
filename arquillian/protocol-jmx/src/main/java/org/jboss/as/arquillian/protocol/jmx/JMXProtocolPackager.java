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

import static org.jboss.as.osgi.deployment.OSGiXServiceParseProcessor.XSERVICE_PROPERTIES_NAME;

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

import org.jboss.arquillian.container.test.spi.TestDeployment;
import org.jboss.arquillian.container.test.spi.client.deployment.DeploymentPackager;
import org.jboss.arquillian.container.test.spi.client.deployment.ProtocolArchiveProcessor;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.protocol.jmx.JMXProtocol;
import org.jboss.as.arquillian.protocol.jmx.JMXProtocolAS7.ServiceArchiveHolder;
import org.jboss.as.arquillian.service.ArquillianService;
import org.jboss.as.arquillian.service.JMXProtocolEndpointExtension;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.osgi.framework.Constants;
import org.jboss.osgi.testing.ManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * A {@link DeploymentPackager} for the JMXProtocol.
 *
 * It dynamically generates the arquillian-archive, which is deployed to the
 * target container before the test run.
 *
 * @see ArquillianServiceDeployer
 *
 * @author Thomas.Diesler@jboss.com
 * @since 01-Jul-2011
 */
public class JMXProtocolPackager implements DeploymentPackager {

    private static final Logger log = Logger.getLogger(JMXProtocolPackager.class);

    private ServiceArchiveHolder archiveHolder;

    JMXProtocolPackager(ServiceArchiveHolder archiveHolder) {
        this.archiveHolder = archiveHolder;
    }

    @Override
    public Archive<?> generateDeployment(TestDeployment testDeployment, Collection<ProtocolArchiveProcessor> protocolProcessors) {
        final Archive<?> appArchive = testDeployment.getApplicationArchive();
        if (archiveHolder.getArchive() == null) {
            final Collection<Archive<?>> auxArchives = testDeployment.getAuxiliaryArchives();
            JavaArchive archive = generateArquillianServiceArchive(auxArchives);
            archiveHolder.setArchive(archive);
        }
        return appArchive;
    }

    private JavaArchive generateArquillianServiceArchive(Collection<Archive<?>> auxArchives) {

        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "arquillian-service");
        log.debugf("Generating: %s", archive.getName());

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
            log.debugf("Merging archive: %s", aux);
            archive.merge(aux);
        }
        loadableExtensions.add(JMXProtocolEndpointExtension.class.getName());

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

        // Add META-INF/jbosgi-xservice.properties which registers the arquillian service with the OSGi layer
        // Generated default imports for OSGi tests are defined in {@link AbstractOSGiApplicationArchiveProcessor}
        StringBuffer props = new StringBuffer(Constants.BUNDLE_SYMBOLICNAME + ": " + archive.getName() + "\n");
        props.append(Constants.EXPORT_PACKAGE + ": org.jboss.arquillian.container.test.api,org.jboss.arquillian.junit,org.jboss.arquillian.test.api,org.jboss.osgi.testing,");
        props.append("org.jboss.shrinkwrap.api,org.jboss.shrinkwrap.api.asset,org.jboss.shrinkwrap.api.spec,");
        props.append("org.junit,org.junit.runner,javax.inject,org.osgi.framework");
        archive.add(new StringAsset(props.toString()), XSERVICE_PROPERTIES_NAME);

        // Replace the loadable extensions with the collected set
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

        log.debugf("Loadable extensions: %s", loadableExtensions);
        log.debugf("Archive content: %s\n%s", archive, archive.toString(true));
        return archive;
    }
}
