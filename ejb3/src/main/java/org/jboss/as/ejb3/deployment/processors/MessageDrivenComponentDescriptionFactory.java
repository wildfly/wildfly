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

package org.jboss.as.ejb3.deployment.processors;

import static org.jboss.as.ejb3.deployment.processors.AbstractDeploymentUnitProcessor.getEjbJarDescription;
import static org.jboss.as.ejb3.deployment.processors.ViewInterfaces.getPotentialViewInterfaces;

import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.ejb.MessageDriven;
import javax.jms.MessageListener;

import org.jboss.as.ee.component.DeploymentDescriptorEnvironment;
import org.jboss.as.ee.metadata.MetadataCompleteMarker;
import org.jboss.as.ee.structure.EJBAnnotationPropertyReplacement;
import org.jboss.as.ejb3.component.messagedriven.DefaultResourceAdapterService;
import org.jboss.as.ejb3.component.messagedriven.MessageDrivenComponentDescription;
import org.jboss.as.ejb3.deployment.EjbJarDescription;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.util.MdbValidationsUtil;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.EjbDeploymentMarker;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.metadata.ejb.jboss.ejb3.JBossGenericBeanMetaData;
import org.jboss.metadata.ejb.spec.ActivationConfigMetaData;
import org.jboss.metadata.ejb.spec.ActivationConfigPropertiesMetaData;
import org.jboss.metadata.ejb.spec.ActivationConfigPropertyMetaData;
import org.jboss.metadata.ejb.spec.EnterpriseBeanMetaData;
import org.jboss.metadata.ejb.spec.MessageDrivenBeanMetaData;
import org.jboss.metadata.property.PropertyReplacer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * User: jpai
 */
public class MessageDrivenComponentDescriptionFactory extends EJBComponentDescriptionFactory {

    private static final DotName MESSAGE_DRIVEN_ANNOTATION_NAME = DotName.createSimple(MessageDriven.class.getName());
    private final boolean defaultMdbPoolAvailable;

    public MessageDrivenComponentDescriptionFactory(final boolean appclient, final boolean defaultMdbPoolAvailable) {
        super(appclient);
        this.defaultMdbPoolAvailable = defaultMdbPoolAvailable;
    }

    @Override
    protected void processAnnotations(DeploymentUnit deploymentUnit, CompositeIndex compositeIndex) throws DeploymentUnitProcessingException {
        if (MetadataCompleteMarker.isMetadataComplete(deploymentUnit)) {
            return;
        }

        processMessageBeans(deploymentUnit, compositeIndex.getAnnotations(MESSAGE_DRIVEN_ANNOTATION_NAME), compositeIndex);
    }

    @Override
    protected void processBeanMetaData(final DeploymentUnit deploymentUnit, final EnterpriseBeanMetaData enterpriseBeanMetaData) throws DeploymentUnitProcessingException {
        if (enterpriseBeanMetaData.isMessageDriven()) {
            assert enterpriseBeanMetaData instanceof MessageDrivenBeanMetaData : enterpriseBeanMetaData + " is not a MessageDrivenBeanMetaData";
            processMessageDrivenBeanMetaData(deploymentUnit, (MessageDrivenBeanMetaData) enterpriseBeanMetaData);
        }
    }

    private void processMessageBeans(final DeploymentUnit deploymentUnit, final Collection<AnnotationInstance> messageBeanAnnotations, final CompositeIndex compositeIndex) throws DeploymentUnitProcessingException {
        if (messageBeanAnnotations.isEmpty())
            return;

        final EjbJarDescription ejbJarDescription = getEjbJarDescription(deploymentUnit);
        final PropertyReplacer propertyReplacer = EJBAnnotationPropertyReplacement.propertyReplacer(deploymentUnit);
        final ServiceName deploymentUnitServiceName = deploymentUnit.getServiceName();
        DeploymentDescriptorEnvironment deploymentDescriptorEnvironment = null;

        for (final AnnotationInstance messageBeanAnnotation : messageBeanAnnotations) {
            final AnnotationTarget target = messageBeanAnnotation.target();
            final ClassInfo beanClassInfo = (ClassInfo) target;
            if (! MdbValidationsUtil.assertMDBClassValidity(beanClassInfo).isEmpty() ) {
                continue;
            }
            final String ejbName = beanClassInfo.name().local();
            final AnnotationValue nameValue = messageBeanAnnotation.value("name");
            final String beanName = (nameValue == null || nameValue.asString().isEmpty()) ? ejbName : propertyReplacer.replaceProperties(nameValue.asString());
            final MessageDrivenBeanMetaData beanMetaData = getEnterpriseBeanMetaData(deploymentUnit, beanName, MessageDrivenBeanMetaData.class);
            final String beanClassName;
            final String messageListenerInterfaceName;
            final Properties activationConfigProperties = getActivationConfigProperties(messageBeanAnnotation, propertyReplacer);
            final String messagingType;
            if (beanMetaData != null) {
                beanClassName = override(beanClassInfo.name().toString(), beanMetaData.getEjbClass());
                deploymentDescriptorEnvironment = new DeploymentDescriptorEnvironment("java:comp/env/", beanMetaData);

                if (beanMetaData instanceof MessageDrivenBeanMetaData) {
                    //It may actually be GenericBeanMetadata instance
                    final MessageDrivenBeanMetaData mdb = (MessageDrivenBeanMetaData) beanMetaData;
                    messagingType = mdb.getMessagingType();
                    final ActivationConfigMetaData activationConfigMetaData = mdb.getActivationConfig();
                    if (activationConfigMetaData != null) {
                        final ActivationConfigPropertiesMetaData propertiesMetaData = activationConfigMetaData.getActivationConfigProperties();
                        if (propertiesMetaData != null) {
                            for (final ActivationConfigPropertyMetaData propertyMetaData : propertiesMetaData) {
                                activationConfigProperties.put(propertyMetaData.getKey(), propertyMetaData.getValue());
                            }
                        }
                    }
                } else if (beanMetaData instanceof JBossGenericBeanMetaData) {
                    //TODO: fix the hierarchy so this is not needed
                    final JBossGenericBeanMetaData mdb = (JBossGenericBeanMetaData) beanMetaData;
                    messagingType = mdb.getMessagingType();
                    final ActivationConfigMetaData activationConfigMetaData = mdb.getActivationConfig();
                    if (activationConfigMetaData != null) {
                        final ActivationConfigPropertiesMetaData propertiesMetaData = activationConfigMetaData.getActivationConfigProperties();
                        if (propertiesMetaData != null) {
                            for (final ActivationConfigPropertyMetaData propertyMetaData : propertiesMetaData) {
                                activationConfigProperties.put(propertyMetaData.getKey(), propertyMetaData.getValue());
                            }
                        }
                    }
                } else {
                    messagingType = null;
                }
                messageListenerInterfaceName = messagingType != null ? messagingType : getMessageListenerInterface(compositeIndex, messageBeanAnnotation);

            } else {
                beanClassName = beanClassInfo.name().toString();
                messageListenerInterfaceName = getMessageListenerInterface(compositeIndex, messageBeanAnnotation);
            }
            final String defaultResourceAdapterName = this.getDefaultResourceAdapterName(deploymentUnit.getServiceRegistry());
            final MessageDrivenComponentDescription beanDescription = new MessageDrivenComponentDescription(beanName, beanClassName, ejbJarDescription, deploymentUnitServiceName, messageListenerInterfaceName, activationConfigProperties, defaultResourceAdapterName, beanMetaData, defaultMdbPoolAvailable);
            beanDescription.setDeploymentDescriptorEnvironment(deploymentDescriptorEnvironment);

            addComponent(deploymentUnit, beanDescription);
        }

        EjbDeploymentMarker.mark(deploymentUnit);
    }

    private String getMessageListenerInterface(final CompositeIndex compositeIndex, final AnnotationInstance messageBeanAnnotation) throws DeploymentUnitProcessingException {
        final AnnotationValue value = messageBeanAnnotation.value("messageListenerInterface");
        if (value != null)
            return value.asClass().name().toString();
        final ClassInfo beanClass = (ClassInfo) messageBeanAnnotation.target();
        final Set<DotName> interfaces = new HashSet<DotName>(getPotentialViewInterfaces(beanClass));
        // check super class(es) of the bean
        DotName superClassDotName = beanClass.superName();
        while (interfaces.isEmpty() && superClassDotName != null && !superClassDotName.toString().equals(Object.class.getName())) {
            final ClassInfo superClass = compositeIndex.getClassByName(superClassDotName);
            if (superClass == null) {
                break;
            }
            interfaces.addAll(getPotentialViewInterfaces(superClass));
            // move to next super class
            superClassDotName = superClass.superName();
        }
        if (interfaces.size() != 1)
            throw EjbLogger.ROOT_LOGGER.mdbDoesNotImplementNorSpecifyMessageListener(beanClass);
        return interfaces.iterator().next().toString();
    }

    private Properties getActivationConfigProperties(final ActivationConfigMetaData activationConfig) {
        final Properties activationConfigProps = new Properties();
        if (activationConfig == null || activationConfig.getActivationConfigProperties() == null) {
            return activationConfigProps;
        }
        final ActivationConfigPropertiesMetaData activationConfigPropertiesMetaData = activationConfig.getActivationConfigProperties();
        for (ActivationConfigPropertyMetaData activationConfigProp : activationConfigPropertiesMetaData) {
            if (activationConfigProp == null) {
                continue;
            }
            final String propName = activationConfigProp.getActivationConfigPropertyName();
            final String propValue = activationConfigProp.getValue();
            if (propName != null) {
                activationConfigProps.put(propName, propValue);
            }
        }
        return activationConfigProps;
    }

    private void processMessageDrivenBeanMetaData(final DeploymentUnit deploymentUnit, final MessageDrivenBeanMetaData mdb) throws DeploymentUnitProcessingException {
        final EjbJarDescription ejbJarDescription = getEjbJarDescription(deploymentUnit);
        final String beanName = mdb.getName();
        final String beanClassName = mdb.getEjbClass();
        String messageListenerInterface = mdb.getMessagingType();
        if (messageListenerInterface == null || messageListenerInterface.trim().isEmpty()) {
            // TODO: This isn't really correct to default to MessageListener
            messageListenerInterface = MessageListener.class.getName();
        }
        final Properties activationConfigProps = getActivationConfigProperties(mdb.getActivationConfig());
        final String defaultResourceAdapterName = this.getDefaultResourceAdapterName(deploymentUnit.getServiceRegistry());
        final MessageDrivenComponentDescription mdbComponentDescription = new MessageDrivenComponentDescription(beanName, beanClassName, ejbJarDescription, deploymentUnit.getServiceName(), messageListenerInterface, activationConfigProps, defaultResourceAdapterName, mdb, defaultMdbPoolAvailable);
        mdbComponentDescription.setDeploymentDescriptorEnvironment(new DeploymentDescriptorEnvironment("java:comp/env/", mdb));
        addComponent(deploymentUnit, mdbComponentDescription);
    }

    private Properties getActivationConfigProperties(final AnnotationInstance messageBeanAnnotation, PropertyReplacer propertyReplacer) {
        final Properties props = new Properties();
        final AnnotationValue activationConfig = messageBeanAnnotation.value("activationConfig");
        if (activationConfig == null)
            return props;
        for (final AnnotationInstance propAnnotation : activationConfig.asNestedArray()) {
            String propertyName = propAnnotation.value("propertyName").asString();
            String propertyValue = propAnnotation.value("propertyValue").asString();
            props.put(propertyReplacer.replaceProperties(propertyName), propertyReplacer.replaceProperties(propertyValue));
        }
        return props;
    }

    /**
     * Returns the name of the resource adapter which will be used as the default RA for MDBs (unless overridden by
     * the MDBs).
     *
     * @param serviceRegistry
     * @return
     */
    private String getDefaultResourceAdapterName(final ServiceRegistry serviceRegistry) {
        if (appclient) {
            // we must report the MDB, but we can't use any MDB/JCA facilities
            return "n/a";
        }
        final ServiceController<DefaultResourceAdapterService> serviceController = (ServiceController<DefaultResourceAdapterService>) serviceRegistry.getRequiredService(DefaultResourceAdapterService.DEFAULT_RA_NAME_SERVICE_NAME);
        return serviceController.getValue().getDefaultResourceAdapterName();
    }

}
