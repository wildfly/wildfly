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

package org.jboss.as.messaging.deployment;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.ee.resource.definition.ResourceDefinitionInjectionSource;
import org.jboss.as.messaging.MessagingExtension;
import org.jboss.as.messaging.MessagingServices;
import org.jboss.as.messaging.jms.JMSQueueConfigurationRuntimeHandler;
import org.jboss.as.messaging.jms.JMSQueueService;
import org.jboss.as.messaging.jms.JMSTopicConfigurationRuntimeHandler;
import org.jboss.as.messaging.jms.JMSTopicService;
import org.jboss.as.naming.ContextListAndJndiViewManagedReferenceFactory;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentResourceSupport;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

import javax.jms.Destination;
import javax.jms.Queue;
import javax.jms.Topic;

import static org.jboss.as.messaging.CommonAttributes.DEFAULT;
import static org.jboss.as.messaging.CommonAttributes.DURABLE;
import static org.jboss.as.messaging.CommonAttributes.ENTRIES;
import static org.jboss.as.messaging.CommonAttributes.HORNETQ_SERVER;
import static org.jboss.as.messaging.CommonAttributes.JMS_QUEUE;
import static org.jboss.as.messaging.CommonAttributes.JMS_TOPIC;
import static org.jboss.as.messaging.CommonAttributes.NAME;
import static org.jboss.as.messaging.CommonAttributes.SELECTOR;
import static org.jboss.as.messaging.logging.MessagingLogger.ROOT_LOGGER;

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
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final String uniqueName = uniqueName(context);
        try {
            ServiceName hqServiceName = MessagingServices.getHornetQServiceName(getHornetQServerName());

            if (interfaceName.equals(Queue.class.getName())) {
                startQueue(uniqueName, phaseContext.getServiceTarget(), hqServiceName, serviceBuilder, deploymentUnit, injector);
            } else {
                startTopic(uniqueName, phaseContext.getServiceTarget(), hqServiceName, serviceBuilder, deploymentUnit, injector);
            }
        } catch (Exception e) {
            throw new DeploymentUnitProcessingException(e);
        }
    }

    /**
     * To workaround HornetQ's BindingRegistry limitation in {@link org.jboss.as.messaging.jms.AS7BindingRegistry}
     * that does not allow to build a BindingInfo with the ResolutionContext info, the JMS queue is created *without* any
     * JNDI bindings and handle the JNDI bindings directly by getting the service's JMS queue.
    */
    private void startQueue(final String queueName,
                            final ServiceTarget serviceTarget,
                            final ServiceName hqServiceName,
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

        Service<Queue> queueService = JMSQueueService.installService(queueName, serviceTarget, hqServiceName, selector, durable, new String[0]);
        inject(serviceBuilder, injector, queueService);

        //create the management registration
        final PathElement serverElement = PathElement.pathElement(HORNETQ_SERVER, getHornetQServerName());
        final PathElement dest = PathElement.pathElement(JMS_QUEUE, queueName);
        final DeploymentResourceSupport deploymentResourceSupport = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_RESOURCE_SUPPORT);
        deploymentResourceSupport.getDeploymentSubModel(MessagingExtension.SUBSYSTEM_NAME, serverElement);
        PathAddress registration = PathAddress.pathAddress(serverElement, dest);
        MessagingXmlInstallDeploymentUnitProcessor.createDeploymentSubModel(registration, deploymentUnit);
        JMSQueueConfigurationRuntimeHandler.INSTANCE.registerResource(getHornetQServerName(), queueName, destination);
    }

    private void startTopic(String topicName,
                            ServiceTarget serviceTarget,
                            ServiceName hqServiceName,
                            ServiceBuilder<?> serviceBuilder,
                            DeploymentUnit deploymentUnit,
                            Injector<ManagedReferenceFactory> injector) {
        ModelNode destination = new ModelNode();
        destination.get(NAME).set(topicName);
        destination.get(ENTRIES).add(jndiName);

        Service<Topic> topicService = JMSTopicService.installService(topicName, hqServiceName, serviceTarget, new String[0]);
        inject(serviceBuilder, injector, topicService);

        //create the management registration
        final PathElement serverElement = PathElement.pathElement(HORNETQ_SERVER, getHornetQServerName());
        final PathElement dest = PathElement.pathElement(JMS_TOPIC, topicName);
        final DeploymentResourceSupport deploymentResourceSupport = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_RESOURCE_SUPPORT);
        deploymentResourceSupport.getDeploymentSubModel(MessagingExtension.SUBSYSTEM_NAME, serverElement);
        PathAddress registration = PathAddress.pathAddress(serverElement, dest);
        MessagingXmlInstallDeploymentUnitProcessor.createDeploymentSubModel(registration, deploymentUnit);
        JMSTopicConfigurationRuntimeHandler.INSTANCE.registerResource(getHornetQServerName(), topicName, destination);
    }

    private <D extends Destination> void inject(ServiceBuilder<?> serviceBuilder, Injector<ManagedReferenceFactory> injector, Service<D> destinationService) {
        final ContextListAndJndiViewManagedReferenceFactory referenceFactoryService = new MessagingJMSDestinationManagedReferenceFactory(destinationService);
        serviceBuilder.addInjection(injector, referenceFactoryService)
                .addListener(new AbstractServiceListener<Object>() {
                    public void transition(final ServiceController<? extends Object> controller, final ServiceController.Transition transition) {
                        switch (transition) {
                            case STARTING_to_UP: {
                                ROOT_LOGGER.boundJndiName(jndiName);
                                break;
                            }
                            case START_REQUESTED_to_DOWN: {
                                ROOT_LOGGER.unboundJndiName(jndiName);
                                break;
                            }
                            case REMOVING_to_REMOVED: {
                                ROOT_LOGGER.debugf("Removed messaging object [%s]", jndiName);
                                break;
                            }
                        }
                    }
                });
    }

    /**
     * The JMS destination can specify another hornetq-server to deploy its destinations
     * by passing a property hornetq-server=&lt;name of the server>. Otherwise, "default" is used by default.
     */
    private String getHornetQServerName() {
        return properties.containsKey(HORNETQ_SERVER) ? properties.get(HORNETQ_SERVER) : DEFAULT;
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
