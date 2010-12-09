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

package org.jboss.as.connector.deployers.processors;

import java.util.List;
import static org.jboss.as.connector.deployers.processors.DataSourcesAttachement.getDataSourcesAttachment;
import org.jboss.as.connector.metadata.xmldescriptors.ConnectorXmlDescriptor;
import org.jboss.as.deployment.Attachments;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.deployment.unit.DeploymentUnit;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.as.deployment.unit.DeploymentPhaseContext;
import org.jboss.jca.common.api.metadata.ds.DataSource;
import org.jboss.jca.common.api.metadata.ds.DataSources;
import org.jboss.jca.common.api.metadata.ds.XaDataSource;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;

/**
 * DeploymentUnitProcessor responsible for using IronJacamar metadata to add Module dependencies for
 * datasource deployments.
 *
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author <a href="jesper.pedersen@jboss.org">Jesper Pedersen</a>
 * @author John Bailey
 */
public class DsDependencyProcessor implements DeploymentUnitProcessor {

    public static final Logger log = Logger.getLogger("org.jboss.as.connector.deployer.dsdeployer");

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final ConnectorXmlDescriptor connectorXmlDescriptor = phaseContext.getAttachment(ConnectorXmlDescriptor.ATTACHMENT_KEY);

        final String deploymentName = connectorXmlDescriptor == null ? null : connectorXmlDescriptor.getDeploymentName();

        final DataSources datasources = getDataSourcesAttachment(phaseContext.getDeploymentUnitContext());
        if (datasources == null || deploymentName == null || !deploymentName.startsWith("jdbc"))
            return;

        log.tracef("Processing datasource deployment: %s", datasources);

         try {
            if (deploymentName.indexOf("local") != -1) {
                // Local datasources
                List<DataSource> dss = datasources.getDataSource();
                if (dss != null && dss.size() > 0) {
                    for (DataSource ds : dss) {
                        try {
                            log.tracef("Processing datasource deployment: %s", ds);

                            if (ds.getModule() != null && !ds.getModule().trim().equals("")) {
                                ModuleIdentifier jdbcIdentifier = ModuleIdentifier.fromString(ds.getModule());
                                // TODO: Use application server module loader
                                Module jdbcModule = Module.getSystemModuleLoader().loadModule(jdbcIdentifier);

                                // Hack: Link the jdbcModule
                                phaseContext.addToAttachmentList(Attachments.MODULE_DEPENDENCIES, new ModuleDependency(jdbcIdentifier, false, false));
                            } else {
                                log.warnf("No module defined for %s", ds.getJndiName());
                            }
                        } catch (ModuleLoadException mle) {
                            log.warnf("Unable to resolve %s for %s", ds.getModule(), ds.getJndiName());
                        }
                    }
                }
            } else {
                // XA datasources
                List<XaDataSource> xadss = datasources.getXaDataSource();
                if (xadss != null && xadss.size() > 0) {
                    for (XaDataSource xads : xadss) {
                        try {
                            log.tracef("Processing xa-datasource deployment: %s", xads);

                            if (xads.getModule() != null && !xads.getModule().trim().equals("")) {
                                ModuleIdentifier jdbcIdentifier = ModuleIdentifier.fromString(xads.getModule());
                                // TODO: Use application server module loader
                                Module jdbcModule = Module.getSystemModuleLoader().loadModule(jdbcIdentifier);

                                // Hack: Link the jdbcModule
                                phaseContext.addToAttachmentList(Attachments.MODULE_DEPENDENCIES, new ModuleDependency(jdbcIdentifier, false, false));
                            } else {
                                log.warnf("No module defined for %s", xads.getJndiName());
                            }
                        } catch (ModuleLoadException mle) {
                            log.warnf("Unable to resolve %s for %s", xads.getModule(), xads.getJndiName());
                        }
                    }
                }
            }
        } catch (Throwable t) {
            throw new DeploymentUnitProcessingException(t);
        }
    }

    public void undeploy(final DeploymentUnit context) {
    }
}
