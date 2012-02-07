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

package org.jboss.as.ejb3.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;

import java.util.EnumSet;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.OperationEntry.Flag;
import org.jboss.as.ejb3.subsystem.deployment.EntityBeanResourceDefinition;
import org.jboss.as.ejb3.subsystem.deployment.MessageDrivenBeanResourceDefinition;
import org.jboss.as.ejb3.subsystem.deployment.SingletonBeanDeploymentResourceDefinition;
import org.jboss.as.ejb3.subsystem.deployment.StatefulSessionBeanDeploymentResourceDefinition;
import org.jboss.as.ejb3.subsystem.deployment.StatelessSessionBeanDeploymentResourceDefinition;
import org.jboss.as.threads.ThreadFactoryResolver;
import org.jboss.as.threads.ThreadsServices;
import org.jboss.as.threads.UnboundedQueueThreadPoolResourceDefinition;

/**
 * Extension that provides the EJB3 subsystem.
 *
 * @author Emanuel Muckenhuber
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class EJB3Extension implements Extension {

    public static final String SUBSYSTEM_NAME = "ejb3";
    public static final String NAMESPACE_1_0 = "urn:jboss:domain:ejb3:1.0";
    public static final String NAMESPACE_1_1 = "urn:jboss:domain:ejb3:1.1";
    public static final String NAMESPACE_1_2 = "urn:jboss:domain:ejb3:1.2";

    private static final String RESOURCE_NAME = EJB3Extension.class.getPackage().getName() + ".LocalDescriptions";

    public static ResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        return new StandardResourceDescriptionResolver(keyPrefix, RESOURCE_NAME, EJB3Extension.class.getClassLoader(), true, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(ExtensionContext context) {

        final boolean registerRuntimeOnly = context.isRuntimeOnlyRegistrationValid();

        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, 1, 0);

        subsystem.registerXMLElementWriter(EJB3Subsystem12Parser.INSTANCE);

        final ManagementResourceRegistration subsystemRegistration = subsystem.registerSubsystemModel(EJB3SubsystemRootResourceDefinition.INSTANCE);

        // describe operation for the subsystem
        subsystemRegistration.registerOperationHandler(DESCRIBE, GenericSubsystemDescribeHandler.INSTANCE, GenericSubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);

        // subsystem=ejb3/service=remote
        subsystemRegistration.registerSubModel(EJB3RemoteResourceDefinition.INSTANCE);

        // subsystem=ejb3/service=async
        subsystemRegistration.registerSubModel(EJB3AsyncResourceDefinition.INSTANCE);

        // subsystem=ejb3/strict-max-bean-instance-pool=*
        subsystemRegistration.registerSubModel(StrictMaxPoolResourceDefinition.INSTANCE);

        subsystemRegistration.registerSubModel(CacheFactoryResourceDefinition.INSTANCE);
        subsystemRegistration.registerSubModel(FilePassivationStoreResourceDefinition.INSTANCE);
        subsystemRegistration.registerSubModel(ClusterPassivationStoreResourceDefinition.INSTANCE);

        // subsystem=ejb3/service=timerservice
        subsystemRegistration.registerSubModel(TimerServiceResourceDefinition.INSTANCE);

        // subsystem=ejb3/thread-pool=*
        subsystemRegistration.registerSubModel(UnboundedQueueThreadPoolResourceDefinition.create(EJB3SubsystemModel.THREAD_POOL,
                new EJB3ThreadFactoryResolver(), EJB3SubsystemModel.BASE_THREAD_POOL_SERVICE_NAME, registerRuntimeOnly));

        // subsystem=ejb3/service=iiop
        subsystemRegistration.registerSubModel(EJB3IIOPResourceDefinition.INSTANCE);

        if (registerRuntimeOnly) {
            ResourceDefinition deploymentsDef = new SimpleResourceDefinition(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME),
                    getResourceDescriptionResolver("deployed"));
            final ManagementResourceRegistration deploymentsRegistration = subsystem.registerDeploymentModel(deploymentsDef);
            deploymentsRegistration.registerSubModel(EntityBeanResourceDefinition.INSTANCE);
            deploymentsRegistration.registerSubModel(MessageDrivenBeanResourceDefinition.INSTANCE);
            deploymentsRegistration.registerSubModel(SingletonBeanDeploymentResourceDefinition.INSTANCE);
            deploymentsRegistration.registerSubModel(StatelessSessionBeanDeploymentResourceDefinition.INSTANCE);
            deploymentsRegistration.registerSubModel(StatefulSessionBeanDeploymentResourceDefinition.INSTANCE);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE_1_0, EJB3Subsystem10Parser.INSTANCE);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE_1_1, EJB3Subsystem11Parser.INSTANCE);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE_1_2, EJB3Subsystem12Parser.INSTANCE);
    }

    private static class EJB3ThreadFactoryResolver extends ThreadFactoryResolver.SimpleResolver {

        private EJB3ThreadFactoryResolver() {
            super(ThreadsServices.FACTORY);
        }

        @Override
        protected String getThreadGroupName(String threadPoolName) {
            return "EJB " + threadPoolName;
        }
    }
}
