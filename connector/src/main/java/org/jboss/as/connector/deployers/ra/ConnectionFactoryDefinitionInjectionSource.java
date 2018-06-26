/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.connector.deployers.ra;

import org.jboss.as.connector.services.mdr.AS7MetadataRepository;
import org.jboss.as.connector.services.resourceadapters.ConnectionFactoryReferenceFactoryService;
import org.jboss.as.connector.services.resourceadapters.DirectConnectionFactoryActivatorService;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.ee.resource.definition.ResourceDefinitionInjectionSource;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.modules.Module;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.LifecycleEvent;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

import javax.resource.spi.TransactionSupport;

import static org.jboss.as.connector.logging.ConnectorLogger.DEPLOYMENT_CONNECTOR_LOGGER;
import static org.jboss.as.connector.logging.ConnectorLogger.SUBSYSTEM_RA_LOGGER;

/**
 * A binding description for ConnectionFactoryDefinition annotations.
 * <p/>
 * The referenced connection factory must be directly visible to the
 * component declaring the annotation.
 *
 * @author Jesper Pedersen
 */
public class ConnectionFactoryDefinitionInjectionSource extends ResourceDefinitionInjectionSource {

    public static final String DESCRIPTION = "description";
    public static final String MAX_POOL_SIZE = "maxPoolSize";
    public static final String MIN_POOL_SIZE = "minPoolSize";
    public static final String TRANSACTION_SUPPORT = "transactionSupport";

    private final String interfaceName;
    private final String resourceAdapter;

    private String description;
    private int maxPoolSize = -1;
    private int minPoolSize = -1;

    private TransactionSupport.TransactionSupportLevel transactionSupport;

    public ConnectionFactoryDefinitionInjectionSource(final String jndiName, final String interfaceName, final String resourceAdapter) {
        super(jndiName);
        this.interfaceName = interfaceName;
        this.resourceAdapter = resourceAdapter;
    }

    public void addProperty(String key, String value) {
        properties.put(key, value);
    }

    public void getResourceValue(final ResolutionContext context, final ServiceBuilder<?> serviceBuilder, final DeploymentPhaseContext phaseContext, final Injector<ManagedReferenceFactory> injector) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        String raId = resourceAdapter;

        if (resourceAdapter.startsWith("#")) {
            raId = deploymentUnit.getParent().getName() + raId;
        }
        String deployerServiceName = raId;
        if(! raId.endsWith(".rar")) {
            deployerServiceName = deployerServiceName + ".rar";
            raId = deployerServiceName;
        }
        SUBSYSTEM_RA_LOGGER.debugf("@ConnectionFactoryDefinition: %s for %s binding to %s ", interfaceName, resourceAdapter, jndiName);


        ContextNames.BindInfo bindInfo = ContextNames.bindInfoForEnvEntry(context.getApplicationName(), context.getModuleName(), context.getComponentName(), !context.isCompUsesModule(), jndiName);

        DirectConnectionFactoryActivatorService service = new DirectConnectionFactoryActivatorService(jndiName, interfaceName, resourceAdapter,
                                                                    raId, maxPoolSize, minPoolSize,
                                                                    properties, transactionSupport,
                                                                    module, bindInfo);
        ServiceName serviceName =  DirectConnectionFactoryActivatorService.SERVICE_NAME_BASE.append(jndiName);
        phaseContext.getServiceTarget().addService(serviceName, service)
                .addDependency(ConnectorServices.IRONJACAMAR_MDR, AS7MetadataRepository.class, service.getMdrInjector())
                .addDependency(ConnectorServices.RESOURCE_ADAPTER_DEPLOYER_SERVICE_PREFIX.append(deployerServiceName))
                .setInitialMode(ServiceController.Mode.ACTIVE).install();

        serviceBuilder.addDependency(ConnectionFactoryReferenceFactoryService.SERVICE_NAME_BASE.append(bindInfo.getBinderServiceName()), ManagedReferenceFactory.class, injector);
        serviceBuilder.addListener(new LifecycleListener() {
            public void handleEvent(final ServiceController<?> controller, final LifecycleEvent event) {
                switch (event) {
                    case UP: {
                        DEPLOYMENT_CONNECTOR_LOGGER.connectionFactoryAnnotation(jndiName);
                        break;
                    }
                    case DOWN: {
                        break;
                    }
                    case REMOVED: {
                        DEPLOYMENT_CONNECTOR_LOGGER.debugf("Removed JCA ConnectionFactory [%s]", jndiName);
                    }
                }
            }
        });


    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getMinPoolSize() {
        return minPoolSize;
    }

    public void setMinPoolSize(int minPoolSize) {
        this.minPoolSize = minPoolSize;
    }

    public TransactionSupport.TransactionSupportLevel getTransactionSupportLevel() {
        return transactionSupport;
    }

    public void setTransactionSupportLevel(TransactionSupport.TransactionSupportLevel transactionSupport) {
        this.transactionSupport = transactionSupport;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ConnectionFactoryDefinitionInjectionSource that = (ConnectionFactoryDefinitionInjectionSource) o;

        if (maxPoolSize != that.maxPoolSize) return false;
        if (minPoolSize != that.minPoolSize) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (interfaceName != null ? !interfaceName.equals(that.interfaceName) : that.interfaceName != null)
            return false;
        if (resourceAdapter != null ? !resourceAdapter.equals(that.resourceAdapter) : that.resourceAdapter != null)
            return false;
        if (transactionSupport != that.transactionSupport) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (interfaceName != null ? interfaceName.hashCode() : 0);
        result = 31 * result + (resourceAdapter != null ? resourceAdapter.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + maxPoolSize;
        result = 31 * result + minPoolSize;
        result = 31 * result + (transactionSupport != null ? transactionSupport.hashCode() : 0);
        return result;
    }
}
