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

package org.wildfly.extension.messaging.activemq.deployment;

import static org.wildfly.extension.messaging.activemq.CommonAttributes.DURABLE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.ENTRIES;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JMS_QUEUE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JMS_TOPIC;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.NAME;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SELECTOR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SERVER;
import static org.wildfly.extension.messaging.activemq.deployment.JMSConnectionFactoryDefinitionInjectionSource.getActiveMQServerName;
import static org.wildfly.extension.messaging.activemq.deployment.JMSConnectionFactoryDefinitionInjectionSource.targetsPooledConnectionFactory;
import static org.wildfly.extension.messaging.activemq.logging.MessagingLogger.ROOT_LOGGER;

import java.util.Map;

import javax.jms.Destination;
import javax.jms.Queue;
import javax.jms.Topic;

import org.jboss.as.connector.deployers.ra.AdministeredObjectDefinitionInjectionSource;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.ee.resource.definition.ResourceDefinitionInjectionSource;
import org.jboss.as.naming.ContextListAndJndiViewManagedReferenceFactory;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentResourceSupport;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.LifecycleEvent;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.extension.messaging.activemq.MessagingExtension;
import org.wildfly.extension.messaging.activemq.MessagingServices;
import org.wildfly.extension.messaging.activemq.jms.JMSQueueConfigurationRuntimeHandler;
import org.wildfly.extension.messaging.activemq.jms.JMSQueueService;
import org.wildfly.extension.messaging.activemq.jms.JMSTopicConfigurationRuntimeHandler;
import org.wildfly.extension.messaging.activemq.jms.JMSTopicService;
import org.wildfly.extension.messaging.activemq.jms.WildFlyBindingRegistry;

/**
 * A binding description for JMS Destination definitions.
 * <p/>
 * The referenced JMS definition must be directly visible to the
 * component declaring the annotation.

 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 * @author Eduardo Martins
 */
public class JMSDestinationDefinitionInjectionSource extends ResourceDefinitionInjectionSource {

    private final String interfaceName;

    // optional attributes
    private String description;
    private String className;
    private String resourceAdapter;
    private String destinationName;

    public JMSDestinationDefinitionInjectionSource(final String jndiName, String interfaceName) {
        super(jndiName);
        this.interfaceName = interfaceName;
    }

    void setDescription(String description) {
        this.description = description;
    }

    void setClassName(String className) {
        this.className = className;
    }

    void setResourceAdapter(String resourceAdapter) {
        this.resourceAdapter = resourceAdapter;
    }

    void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }

    private String uniqueName(InjectionSource.ResolutionContext context) {
        if (destinationName != null && !destinationName.isEmpty()) {
            return destinationName;
        }

        StringBuilder uniqueName = new StringBuilder();
        uniqueName.append(context.getApplicationName() + "_");
        uniqueName.append(context.getModuleName() + "_");
        if (context.getComponentName() != null) {
            uniqueName.append(context.getComponentName() + "_");
        }
        uniqueName.append(jndiName);
        return uniqueName.toString();
    }

    public void getResourceValue(final InjectionSource.ResolutionContext context, final ServiceBuilder<?> serviceBuilder, final DeploymentPhaseContext phaseContext, final Injector<ManagedReferenceFactory> injector) throws DeploymentUnitProcessingException {
        if (targetsPooledConnectionFactory(getActiveMQServerName(properties), resourceAdapter, phaseContext.getServiceRegistry())) {
            startActiveMQDestination(context, serviceBuilder, phaseContext, injector);
        } else {
            // delegate to the resource-adapter subsystem to create a generic JCA admin object.
            AdministeredObjectDefinitionInjectionSource aodis = new AdministeredObjectDefinitionInjectionSource(jndiName, className, resourceAdapter);
            aodis.setInterface(interfaceName);
            aodis.setDescription(description);
            // transfer all the generic properties
            for (Map.Entry<String, String> property : properties.entrySet()) {
                aodis.addProperty(property.getKey(), property.getValue());
            }
            aodis.getResourceValue(context, serviceBuilder, phaseContext, injector);

        }
    }

    private void startActiveMQDestination(ResolutionContext context, ServiceBuilder<?> serviceBuilder, DeploymentPhaseContext phaseContext, Injector<ManagedReferenceFactory> injector) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final String uniqueName = uniqueName(context);
        try {
            ServiceName serviceName = MessagingServices.getActiveMQServiceName(getActiveMQServerName(properties));

            if (interfaceName.equals(Queue.class.getName())) {
                startQueue(uniqueName, phaseContext.getServiceTarget(), serviceName, serviceBuilder, deploymentUnit, injector);
            } else {
                startTopic(uniqueName, phaseContext.getServiceTarget(), serviceName, serviceBuilder, deploymentUnit, injector);
            }
        } catch (Exception e) {
            throw new DeploymentUnitProcessingException(e);
        }
    }

    /**
     * To workaround ActiveMQ's BindingRegistry limitation in {@link WildFlyBindingRegistry}
     * that does not allow to build a BindingInfo with the ResolutionContext info, the JMS queue is created *without* any
     * JNDI bindings and handle the JNDI bindings directly by getting the service's JMS queue.
    */
    private void startQueue(final String queueName,
                            final ServiceTarget serviceTarget,
                            final ServiceName serverServiceName,
                            final ServiceBuilder<?> serviceBuilder,
                            final DeploymentUnit deploymentUnit,
                            final Injector<ManagedReferenceFactory>  injector) {

        final String selector = properties.containsKey(SELECTOR.getName()) ? properties.get(SELECTOR.getName()) : null;
        final boolean durable = properties.containsKey(DURABLE.getName()) ? Boolean.valueOf(properties.get(DURABLE.getName())) : DURABLE.getDefaultValue().asBoolean();

        ModelNode destination = new ModelNode();
        destination.get(NAME).set(queueName);
        destination.get(DURABLE.getName()).set(durable);
        if (selector != null) {
            destination.get(SELECTOR.getName()).set(selector);
        }
        destination.get(ENTRIES).add(jndiName);

        Service<Queue> queueService = JMSQueueService.installService(queueName, serviceTarget, serverServiceName, selector, durable);
        inject(serviceBuilder, injector, queueService);

        //create the management registration
        String serverName = getActiveMQServerName(properties);
        final PathElement serverElement = PathElement.pathElement(SERVER, serverName);
        final PathElement dest = PathElement.pathElement(JMS_QUEUE, queueName);
        final DeploymentResourceSupport deploymentResourceSupport = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_RESOURCE_SUPPORT);
        deploymentResourceSupport.getDeploymentSubModel(MessagingExtension.SUBSYSTEM_NAME, serverElement);
        PathAddress registration = PathAddress.pathAddress(serverElement, dest);
        MessagingXmlInstallDeploymentUnitProcessor.createDeploymentSubModel(registration, deploymentUnit);
        JMSQueueConfigurationRuntimeHandler.INSTANCE.registerResource(serverName, queueName, destination);
    }

    private void startTopic(String topicName,
                            ServiceTarget serviceTarget,
                            ServiceName serverServiceName,
                            ServiceBuilder<?> serviceBuilder,
                            DeploymentUnit deploymentUnit,
                            Injector<ManagedReferenceFactory> injector) {
        ModelNode destination = new ModelNode();
        destination.get(NAME).set(topicName);
        destination.get(ENTRIES).add(jndiName);

        Service<Topic> topicService = JMSTopicService.installService(topicName, serverServiceName, serviceTarget);
        inject(serviceBuilder, injector, topicService);

        //create the management registration
        String serverName = getActiveMQServerName(properties);
        final PathElement serverElement = PathElement.pathElement(SERVER, serverName);
        final PathElement dest = PathElement.pathElement(JMS_TOPIC, topicName);
        final DeploymentResourceSupport deploymentResourceSupport = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_RESOURCE_SUPPORT);
        deploymentResourceSupport.getDeploymentSubModel(MessagingExtension.SUBSYSTEM_NAME, serverElement);
        PathAddress registration = PathAddress.pathAddress(serverElement, dest);
        MessagingXmlInstallDeploymentUnitProcessor.createDeploymentSubModel(registration, deploymentUnit);
        JMSTopicConfigurationRuntimeHandler.INSTANCE.registerResource(serverName, topicName, destination);
    }

    private <D extends Destination> void inject(ServiceBuilder<?> serviceBuilder, Injector<ManagedReferenceFactory> injector, Service<D> destinationService) {
        final ContextListAndJndiViewManagedReferenceFactory referenceFactoryService = new MessagingJMSDestinationManagedReferenceFactory(destinationService);
        serviceBuilder.addInjection(injector, referenceFactoryService)
                .addListener(new LifecycleListener() {
                    public void handleEvent(final ServiceController<?> controller, final LifecycleEvent event) {
                        switch (event) {
                            case UP: {
                                ROOT_LOGGER.boundJndiName(jndiName);
                                break;
                            }
                            case DOWN: {
                                ROOT_LOGGER.unboundJndiName(jndiName);
                                break;
                            }
                            case REMOVED: {
                                ROOT_LOGGER.debugf("Removed messaging object [%s]", jndiName);
                                break;
                            }
                        }
                    }
                });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        JMSDestinationDefinitionInjectionSource that = (JMSDestinationDefinitionInjectionSource) o;

        if (className != null ? !className.equals(that.className) : that.className != null) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (destinationName != null ? !destinationName.equals(that.destinationName) : that.destinationName != null)
            return false;
        if (interfaceName != null ? !interfaceName.equals(that.interfaceName) : that.interfaceName != null)
            return false;
        if (resourceAdapter != null ? !resourceAdapter.equals(that.resourceAdapter) : that.resourceAdapter != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (interfaceName != null ? interfaceName.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (className != null ? className.hashCode() : 0);
        result = 31 * result + (resourceAdapter != null ? resourceAdapter.hashCode() : 0);
        result = 31 * result + (destinationName != null ? destinationName.hashCode() : 0);
        return result;
    }
}
