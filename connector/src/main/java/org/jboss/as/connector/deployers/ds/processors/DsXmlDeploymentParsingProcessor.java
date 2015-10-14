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

package org.jboss.as.connector.deployers.ds.processors;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.jboss.as.connector.deployers.Util;
import org.jboss.as.connector.deployers.ds.DsXmlParser;
import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.jca.common.api.metadata.ds.DataSource;
import org.jboss.jca.common.api.metadata.ds.DataSources;
import org.jboss.metadata.property.PropertyReplacer;
import org.jboss.metadata.property.PropertyResolver;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;

/**
 * Picks up -ds.xml deployments
 *
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public class DsXmlDeploymentParsingProcessor implements DeploymentUnitProcessor {


    static final AttachmentKey<AttachmentList<DataSources>> DATA_SOURCES_ATTACHMENT_KEY = AttachmentKey.createList(DataSources.class);

    private static final String[] LOCATIONS = {"WEB-INF", "META-INF"};

    /**
     * Construct a new instance.
     */
    public DsXmlDeploymentParsingProcessor() {
    }

    /**
     * Process a deployment for standard ra deployment files. Will parse the xml
     * file and attach a configuration discovered during processing.
     *
     * @param phaseContext the deployment unit context
     * @throws DeploymentUnitProcessingException
     *
     */
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        boolean resolveProperties = Util.shouldResolveJBoss(deploymentUnit);
        final PropertyResolver propertyResolver = deploymentUnit.getAttachment(org.jboss.as.ee.metadata.property.Attachments.FINAL_PROPERTY_RESOLVER);
        final PropertyReplacer propertyReplacer = deploymentUnit.getAttachment(org.jboss.as.ee.metadata.property.Attachments.FINAL_PROPERTY_REPLACER);

        final Set<VirtualFile> files = dataSources(deploymentUnit);
        boolean loggedDeprication = false;
        for (VirtualFile f : files) {
            InputStream xmlStream = null;
            try {
                xmlStream = new FileInputStream(f.getPhysicalFile());
                DsXmlParser parser = new DsXmlParser(propertyResolver, propertyReplacer);
                parser.setSystemPropertiesResolved(resolveProperties);
                DataSources dataSources = parser.parse(xmlStream);

                if (dataSources != null) {
                    if (!loggedDeprication) {
                        loggedDeprication = true;
                        ConnectorLogger.ROOT_LOGGER.deprecated();
                    }
                    for (DataSource ds : dataSources.getDataSource()) {
                        if (ds.getDriver() == null) {
                            throw ConnectorLogger.ROOT_LOGGER.FailedDeployDriverNotSpecified(ds.getJndiName());
                        }
                    }
                    deploymentUnit.addToAttachmentList(DATA_SOURCES_ATTACHMENT_KEY, dataSources);
                }
            } catch (Exception e) {
                throw new DeploymentUnitProcessingException(e.getMessage(), e);
            } finally {
                VFSUtils.safeClose(xmlStream);
            }
        }
    }

    private Set<VirtualFile> dataSources(final DeploymentUnit deploymentUnit) {
        final VirtualFile deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
        if (deploymentRoot == null || !deploymentRoot.exists()) {
            return Collections.emptySet();
        }

        final String deploymentRootName = deploymentRoot.getName().toLowerCase(Locale.ENGLISH);

        if (deploymentRootName.endsWith("-ds.xml")) {
            return Collections.singleton(deploymentRoot);
        }
        final Set<VirtualFile> ret = new HashSet<VirtualFile>();
        for (String location : LOCATIONS) {
            final VirtualFile loc = deploymentRoot.getChild(location);
            if (loc.exists()) {
                for (final VirtualFile file : loc.getChildren()) {
                    if (file.getName().endsWith("-ds.xml")) {
                        ret.add(file);
                    }
                }
            }
        }
        return ret;
    }

    public void undeploy(final DeploymentUnit context) {
    }
}
