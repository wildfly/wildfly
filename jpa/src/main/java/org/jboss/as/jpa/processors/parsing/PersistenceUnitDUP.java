/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jpa.processors.parsing;

import org.jboss.as.jpa.config.PersistenceMetadataHolder;
import org.jboss.as.jpa.config.parser.application.PersistenceUnitXmlParser;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.metadata.parser.util.NoopXmlResolver;
import org.jboss.vfs.VirtualFile;

import javax.persistence.spi.PersistenceUnitInfo;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Persistence unit deployment unit processor
 *
 * @author Scott Marlow
 */
public class PersistenceUnitDUP implements DeploymentUnitProcessor {

    private static final String WEB_PERSISTENCE_XML = "WEB-INF/classes/META-INF/persistence.xml";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        handleWarDeployment(phaseContext);

        // TODO:  handle an EJB-JAR file
        // handleEJBDeployment

        // TODO:  handle jar file in the EAR library directory

        // TODO:  application client deployment (probably should be a separate class)
        // handle client deployment


    }

    @Override
    public void undeploy(DeploymentUnit context) {
        // TODO:  undeploy
    }

    private PersistenceMetadataHolder flatten(List<PersistenceMetadataHolder>listPUHolders) {
        // eliminate duplicates (keeping the first instance of each PU by name)
        Map<String, PersistenceUnitInfo> flattened = new HashMap<String,PersistenceUnitInfo>();
        for (PersistenceMetadataHolder puHolder : listPUHolders ) {
            for (PersistenceUnitInfo pu: puHolder.getPersistenceUnits()) {
                if(!flattened.containsKey(pu.getPersistenceUnitName()))
                    flattened.put(pu.getPersistenceUnitName(), pu);
            }
        }
        PersistenceMetadataHolder holder = new PersistenceMetadataHolder();
        holder.setPersistenceUnits(new ArrayList<PersistenceUnitInfo>(flattened.values()));
        return holder;
    }

    private void handleWarDeployment(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (isWarDeployment(deploymentUnit)) {
            // ordered list of PUs
            List<PersistenceMetadataHolder> listPUHolders = new ArrayList<PersistenceMetadataHolder>(1);

            // handle WEB-INF/classes/META-INF/persistence.xml
            final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
            VirtualFile persistence_xml = deploymentRoot.getRoot().getChild(WEB_PERSISTENCE_XML);
            if (persistence_xml.exists()) {
                InputStream is = null;
                try {
                    is = persistence_xml.openStream();
                    final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
                    inputFactory.setXMLResolver(NoopXmlResolver.create());
                    XMLStreamReader xmlReader = inputFactory.createXMLStreamReader(is);
                    PersistenceMetadataHolder puHolder = PersistenceUnitXmlParser.parse(xmlReader);
                    listPUHolders.add(puHolder);
                } catch (Exception e) {
                    throw new DeploymentUnitProcessingException("Failed to parse " + persistence_xml, e);
                } finally {
                    try {
                        if (is != null) {
                            is.close();
                        }
                    } catch (IOException e) {
                        // Ignore
                    }
                }
            }

            // look for persistence.xml in jar files in the WEB-INF/lib directory of a WAR file
            List<ResourceRoot> resourceRoots = deploymentUnit.getAttachment(Attachments.RESOURCE_ROOTS);
            assert resourceRoots != null;
            for (ResourceRoot resourceRoot : resourceRoots) {
                if (resourceRoot.getRoot().getLowerCaseName().endsWith(".jar")) {
                    persistence_xml = resourceRoot.getRoot().getChild(WEB_PERSISTENCE_XML);
                    if (persistence_xml.exists() && persistence_xml.isFile()) {
                        InputStream is = null;
                        try {
                            is = persistence_xml.openStream();
                            final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
                            inputFactory.setXMLResolver(NoopXmlResolver.create());
                            XMLStreamReader xmlReader = inputFactory.createXMLStreamReader(is);
                            PersistenceMetadataHolder puHolder = PersistenceUnitXmlParser.parse(xmlReader);
                            listPUHolders.add(puHolder);
                        } catch (Exception e) {
                            throw new DeploymentUnitProcessingException("Failed to parse " + persistence_xml, e);
                        } finally {
                            try {
                                if (is != null) {
                                    is.close();
                                }
                            } catch (IOException e) {
                                // Ignore
                            }
                        }
                    }
                }
            }
            PersistenceMetadataHolder holder = flatten(listPUHolders);
            // save the persistent unit definitions for the WAR
            deploymentUnit.putAttachment(PersistenceMetadataHolder.PERSISTENCE_UNITS, holder);
        }

    }


    // TODO:
    static boolean isWarDeployment(final DeploymentUnit context) {
        final Boolean result = context.getAttachment(Attachments.WAR_DEPLOYMENT_MARKER);
        return result != null && result;
    }

}
