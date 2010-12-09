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

import static org.jboss.as.connector.deployers.processors.ResourceAdapterAttachment.getResourceAdaptersAttachment;

import java.io.File;
import java.net.URL;
import java.util.Set;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.connector.metadata.deployment.ResourceAdapterXmlDeploymentService;
import org.jboss.as.connector.metadata.xmldescriptors.ConnectorXmlDescriptor;
import org.jboss.as.connector.registry.ResourceAdapterDeploymentRegistry;
import org.jboss.as.connector.subsystems.connector.ConnectorSubsystemConfiguration;
import org.jboss.as.server.deployment.module.ModuleDeploymentProcessor;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.as.deployment.unit.DeploymentPhaseContext;
import org.jboss.as.naming.service.NamingService;
import org.jboss.as.txn.TxnServices;
import org.jboss.jca.common.api.metadata.ironjacamar.IronJacamar;
import org.jboss.jca.common.api.metadata.ra.Connector;
import org.jboss.jca.common.api.metadata.resourceadapter.ResourceAdapters;
import org.jboss.jca.common.metadata.merge.Merger;
import org.jboss.jca.core.spi.mdr.MetadataRepository;
import org.jboss.jca.core.spi.naming.JndiStrategy;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceTarget;

/**
 * DeploymentUnitProcessor responsible for using IronJacamar metadata and create
 * service for ResourceAdapter.
 * @author <a href="mailto:stefano.maestri@redhat.comdhat.com">Stefano
 *         Maestri</a>
 * @author <a href="jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public class RaXmlDeploymentProcessor implements DeploymentUnitProcessor {

    public static final Logger log = Logger.getLogger("org.jboss.as.connector.deployer.raxmldeployer");

    private final MetadataRepository mdr;

    public RaXmlDeploymentProcessor(final MetadataRepository mdr) {
        this.mdr = mdr;
    }

    /**
     * Process a deployment for a Connector. Will install a {@Code
     * JBossService} for this ResourceAdapter.
     * @param phaseContext the deployment unit context
     * @throws DeploymentUnitProcessingException
     */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final ConnectorXmlDescriptor connectorXmlDescriptor = phaseContext.getDeploymentUnitContext().getAttachment(ConnectorXmlDescriptor.ATTACHMENT_KEY);
        if(connectorXmlDescriptor == null) {
            return;  // Skip non ra deployments
        }

        final ResourceAdapters raxmls = getResourceAdaptersAttachment(phaseContext);
        if (raxmls == null)
            return;

        log.tracef("processing Raxml");
        Module module = phaseContext.getAttachment(ModuleDeploymentProcessor.MODULE_ATTACHMENT_KEY);

        if (module == null)
            throw new DeploymentUnitProcessingException("Failed to get module attachment for deployment: " + phaseContext.getName());

        try {
            final ServiceTarget serviceTarget = phaseContext.getBatchBuilder();

            for (org.jboss.jca.common.api.metadata.resourceadapter.ResourceAdapter raxml : raxmls.getResourceAdapters()) {

                String archive = raxml.getArchive();
                URL deployment = null;
                Set<String> deployments = mdr.getResourceAdapters();

                for (String s : deployments) {
                    if (s.endsWith(archive) || s.endsWith(archive.substring(0, archive.indexOf(".rar"))))
                        deployment = new URL(s);
                }

                if (deployment != null) {

                    Connector cmd = mdr.getResourceAdapter(deployment.toExternalForm());
                    IronJacamar ijmd = mdr.getIronJacamar(deployment.toExternalForm());
                    File root = mdr.getRoot(deployment.toExternalForm());

                    cmd = (new Merger()).mergeConnectorWithCommonIronJacamar(raxml, cmd);

                    String deploymentName = archive.substring(0, archive.indexOf(".rar"));

                    ResourceAdapterXmlDeploymentService service = new ResourceAdapterXmlDeploymentService(connectorXmlDescriptor, raxml, cmd, ijmd, module, deploymentName, root);
                    // Create the service
                    serviceTarget
                        .addService(ConnectorServices.RESOURCE_ADAPTER_XML_SERVICE_PREFIX.append(deploymentName), service)
                        .addDependency(ConnectorServices.IRONJACAMAR_MDR, MetadataRepository.class, service.getMdrInjector())
                        .addDependency(ConnectorServices.RESOURCE_ADAPTER_REGISTRY_SERVICE, ResourceAdapterDeploymentRegistry.class, service.getRegistryInjector())
                        .addDependency(ConnectorServices.JNDI_STRATEGY_SERVICE, JndiStrategy.class, service.getJndiInjector())
                        .addDependency(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER, com.arjuna.ats.jbossatx.jta.TransactionManagerService.class, service.getTxmInjector())
                        .addDependency(ConnectorServices.CONNECTOR_CONFIG_SERVICE, ConnectorSubsystemConfiguration.class, service.getConfigInjector())
                        .addDependency(NamingService.SERVICE_NAME)
                        .setInitialMode(Mode.ACTIVE)
                        .install();
                }
            }
        } catch (Throwable t) {
            throw new DeploymentUnitProcessingException(t);
        }
    }
}
