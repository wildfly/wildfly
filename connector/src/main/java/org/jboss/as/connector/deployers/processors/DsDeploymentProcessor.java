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

import static org.jboss.as.connector.deployers.processors.DataSourcesAttachement.getDataSourcesAttachment;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.connector.metadata.xmldescriptors.ConnectorXmlDescriptor;
import org.jboss.as.connector.registry.ResourceAdapterDeploymentRegistry;
import org.jboss.as.connector.subsystems.datasources.DataSourceDeploymentService;
import org.jboss.as.naming.service.NamingService;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.txn.TxnServices;
import org.jboss.jca.common.api.metadata.ds.DataSource;
import org.jboss.jca.common.api.metadata.ds.DataSources;
import org.jboss.jca.common.api.metadata.ds.XaDataSource;
import org.jboss.jca.core.spi.mdr.MetadataRepository;
import org.jboss.jca.core.spi.naming.JndiStrategy;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.util.Strings;

/**
 * DeploymentUnitProcessor responsible for using IronJacamar metadata and create
 * service for ResourceAdapter.
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author <a href="jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public class DsDeploymentProcessor implements DeploymentUnitProcessor {

    public static final Logger log = Logger.getLogger("org.jboss.as.connector.deployer.dsdeployer");

    public DsDeploymentProcessor() {
    }

    /**
     * Deploy datasources
     * @param phaseContext the deployment unit context
     * @throws DeploymentUnitProcessingException
     */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ConnectorXmlDescriptor connectorXmlDescriptor = deploymentUnit
                .getAttachment(ConnectorXmlDescriptor.ATTACHMENT_KEY);

        Module module = deploymentUnit.getAttachment(Attachments.MODULE);

        String deploymentName = connectorXmlDescriptor == null ? null : connectorXmlDescriptor.getDeploymentName();

        DataSources datasources = getDataSourcesAttachment(deploymentUnit);
        if (datasources == null || deploymentName == null || !deploymentName.startsWith("jdbc"))
            return;

        log.tracef("Processing datasource deployement: %s", datasources);

        if (module == null)
            throw new DeploymentUnitProcessingException("Failed to get module attachment for " + deploymentUnit);

        String uniqueJdbcLocalId = null;
        String uniqueJdbcXAId = null;
        boolean shouldDeploy = false;

        if (deploymentName.indexOf("local") != -1) {
            // Local datasources
            List<DataSource> dss = datasources.getDataSource();
            if (dss != null && dss.size() > 0) {
                uniqueJdbcLocalId = deploymentName;
                shouldDeploy = true;
            }
        } else {
            // XA datasources
            List<XaDataSource> xadss = datasources.getXaDataSource();
            if (xadss != null && xadss.size() > 0) {
                uniqueJdbcXAId = deploymentName;
                shouldDeploy = true;
            }
        }

        if (shouldDeploy) {
            final ServiceTarget serviceTarget = phaseContext.getServiceTarget();

            final DataSourceDeploymentService dataSourceDeploymentService = new DataSourceDeploymentService(deploymentName,
                    uniqueJdbcLocalId, uniqueJdbcXAId, datasources, module);
            ServiceBuilder<?> serviceBuilder = serviceTarget
                    .addService(DataSourceDeploymentService.SERVICE_NAME_BASE.append(deploymentName),
                            dataSourceDeploymentService)
                    .addDependency(ConnectorServices.IRONJACAMAR_MDR, MetadataRepository.class,
                            dataSourceDeploymentService.getMdrInjector())
                    .addDependency(ConnectorServices.RESOURCE_ADAPTER_REGISTRY_SERVICE,
                            ResourceAdapterDeploymentRegistry.class, dataSourceDeploymentService.getRegistryInjector())
                    .addDependency(ConnectorServices.JNDI_STRATEGY_SERVICE, JndiStrategy.class,
                            dataSourceDeploymentService.getJndiInjector())
                    .addDependency(TxnServices.JBOSS_TXN_ARJUNA_TRANSACTION_MANAGER,
                            com.arjuna.ats.jbossatx.jta.TransactionManagerService.class,
                            dataSourceDeploymentService.getTxmInjector()).addDependency(NamingService.SERVICE_NAME);
            addJdbcDependency(serviceBuilder, phaseContext);
            serviceBuilder.setInitialMode(ServiceController.Mode.ACTIVE);
            if (uniqueJdbcLocalId != null) {
                serviceBuilder.addDependency(ServiceBuilder.DependencyType.OPTIONAL,
                        ConnectorServices.RESOURCE_ADAPTER_SERVICE_PREFIX.append(uniqueJdbcLocalId));
                serviceBuilder.addDependency(ServiceBuilder.DependencyType.OPTIONAL,
                        ConnectorServices.RESOURCE_ADAPTER_XML_SERVICE_PREFIX.append(uniqueJdbcLocalId));
            } else if (uniqueJdbcXAId != null) {
                serviceBuilder.addDependency(ServiceBuilder.DependencyType.OPTIONAL,
                        ConnectorServices.RESOURCE_ADAPTER_SERVICE_PREFIX.append(uniqueJdbcXAId));
                serviceBuilder.addDependency(ServiceBuilder.DependencyType.OPTIONAL,
                        ConnectorServices.RESOURCE_ADAPTER_XML_SERVICE_PREFIX.append(uniqueJdbcXAId));
            }

            serviceBuilder.install();
        }
    }

    private void addJdbcDependency(ServiceBuilder<?> serviceBuilder, DeploymentPhaseContext phaseContext)
            throws DeploymentUnitProcessingException {
        final ConnectorXmlDescriptor connectorXmlDescriptor = phaseContext.getDeploymentUnit().getAttachment(
                ConnectorXmlDescriptor.ATTACHMENT_KEY);

        final String deploymentName = connectorXmlDescriptor == null ? null : connectorXmlDescriptor.getDeploymentName();

        final DataSources datasources = getDataSourcesAttachment(phaseContext.getDeploymentUnit());
        if (datasources == null || deploymentName == null || !deploymentName.startsWith("jdbc"))
            return;

        log.tracef("Processing datasource deployment: %s", datasources);
        String driverName = null;
        Integer majorVersion = null;
        Integer minorVersion = null;
        List<String> modules = new ArrayList<String>();
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        try {
            if (deploymentName.indexOf("local") != -1) {
                // Local datasources
                List<DataSource> dss = datasources.getDataSource();
                if (dss != null && dss.size() > 0) {
                    for (DataSource ds : dss) {
                        if (ds.getModule() != null && !ds.getModule().trim().equals("")) {
                            modules.add(ds.getModule());
                        } else {
                            log.warnf("No module defined for %s", ds.getJndiName());
                        }
                    }
                }
            } else {
                // XA datasources
                List<XaDataSource> xadss = datasources.getXaDataSource();
                if (xadss != null && xadss.size() > 0) {
                    for (XaDataSource xads : xadss) {
                        log.tracef("Processing xa-datasource deployment: %s", xads);

                        if (xads.getModule() != null && !xads.getModule().trim().equals("")) {
                            modules.add(xads.getModule());
                        } else {
                            log.warnf("No module defined for %s", xads.getJndiName());
                        }
                    }
                }
            }
            for (String module : modules) {
                String[] strings = Strings.split(module, "#");
                if (strings.length != 2) {
                    throw new IllegalArgumentException(
                            "module should define jdbc driver with this format: <driver-name>#<major-version>.<minor-version>");
                }
                driverName = strings[0];
                strings = Strings.split(strings[1], ".", 2);
                if (strings.length != 2) {
                    throw new IllegalArgumentException(
                            "module should define jdbc driver with this format: <driver-name>#<major-version>.<minor-version>");
                }
                try {
                    majorVersion = Integer.valueOf(strings[0]);
                    minorVersion = Integer.valueOf(strings[1]);
                } catch (NumberFormatException nfe) {
                    throw new IllegalArgumentException(
                            "module should define jdbc driver with this format: <driver-name>#<major-version>.<minor-version> "
                                    + "version number should be valid Integer");
                }

                if (driverName != null & majorVersion != null && minorVersion != null) {
                    ServiceName serviceName = ServiceName.JBOSS.append("jdbc-driver", driverName,
                            Integer.toString(majorVersion), Integer.toString(minorVersion));
                    serviceBuilder.addDependency(serviceName);
                } else {
                    break;
                }
            }
        } catch (Throwable t) {
            throw new DeploymentUnitProcessingException(t);
        }

    }

    public void undeploy(DeploymentUnit context) {
        final ConnectorXmlDescriptor connectorXmlDescriptor = context.getAttachment(ConnectorXmlDescriptor.ATTACHMENT_KEY);
        if (connectorXmlDescriptor == null) {
            return;
        }
        final String deploymentName = connectorXmlDescriptor == null ? null : connectorXmlDescriptor.getDeploymentName();
        final ServiceName serviceName = DataSourceDeploymentService.SERVICE_NAME_BASE.append(deploymentName);
        final ServiceController<?> serviceController = context.getServiceRegistry().getService(serviceName);
        if (serviceController != null) {
            serviceController.setMode(ServiceController.Mode.REMOVE);
        }
    }
}
