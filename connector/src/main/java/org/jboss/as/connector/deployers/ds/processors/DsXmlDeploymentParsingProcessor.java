/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
        final PropertyReplacer propertyReplacer = deploymentUnit.getAttachment(org.jboss.as.ee.metadata.property.Attachments.FINAL_PROPERTY_REPLACER);

        final Set<VirtualFile> files = dataSources(deploymentUnit);
        boolean loggedDeprication = false;
        for (VirtualFile f : files) {
            InputStream xmlStream = null;
            try {
                xmlStream = new FileInputStream(f.getPhysicalFile());
                DsXmlParser parser = new DsXmlParser(propertyReplacer);
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
}
