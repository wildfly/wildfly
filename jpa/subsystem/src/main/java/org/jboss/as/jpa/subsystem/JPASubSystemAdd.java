/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.jpa.subsystem;

import static org.jboss.as.jpa.subsystem.JPADefinition.DEFAULT_DATASOURCE;
import static org.jboss.as.jpa.subsystem.JPADefinition.DEFAULT_EXTENDEDPERSISTENCE_INHERITANCE;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.jpa.config.ExtendedPersistenceInheritance;
import org.jboss.as.jpa.persistenceprovider.PersistenceProviderResolverImpl;
import org.jboss.as.jpa.platform.PlatformImpl;
import org.jboss.as.jpa.processor.HibernateSearchProcessor;
import org.jboss.as.jpa.processor.JPAAnnotationProcessor;
import org.jboss.as.jpa.processor.JPAClassFileTransformerProcessor;
import org.jboss.as.jpa.processor.JPADependencyProcessor;
import org.jboss.as.jpa.processor.JPAInterceptorProcessor;
import org.jboss.as.jpa.processor.JPAJarJBossAllParser;
import org.jboss.as.jpa.processor.JpaAttachments;
import org.jboss.as.jpa.processor.PersistenceBeginInstallProcessor;
import org.jboss.as.jpa.processor.PersistenceCompleteInstallProcessor;
import org.jboss.as.jpa.processor.PersistenceRefProcessor;
import org.jboss.as.jpa.processor.PersistenceUnitParseProcessor;
import org.jboss.as.jpa.service.JPAService;
import org.jboss.as.jpa.service.JPAUserTransactionListenerService;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.jbossallxml.JBossAllXmlParserRegisteringProcessor;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceTarget;
import org.jipijapa.cache.spi.Classification;


/**
 * Add the Jakarta Persistence subsystem directive.
 * <p/>
 *
 * @author Scott Marlow
 */

class JPASubSystemAdd extends AbstractBoottimeAddStepHandler {

    public static final JPASubSystemAdd INSTANCE = new JPASubSystemAdd();

    private JPASubSystemAdd() {
        super(DEFAULT_DATASOURCE, DEFAULT_EXTENDEDPERSISTENCE_INHERITANCE);
    }

    protected void performBoottime(final OperationContext context, final ModelNode operation, final ModelNode model) throws
        OperationFailedException {

        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {

                // set Hibernate persistence provider as the default provider
                javax.persistence.spi.PersistenceProviderResolverHolder.setPersistenceProviderResolver(
                    PersistenceProviderResolverImpl.getInstance());
                final boolean appclient = context.getProcessType() == ProcessType.APPLICATION_CLIENT;
                PlatformImpl platform;
                if (appclient) {
                    // we do not yet support a second level cache in the client container
                    platform = new PlatformImpl(Classification.NONE);
                } else {
                    // Infinispan can be used in server container
                    platform = new PlatformImpl(Classification.INFINISPAN, Classification.INFINISPAN);
                }

                processorTarget.addDeploymentProcessor(JPAExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_REGISTER_JBOSS_ALL_JPA,
                        new JBossAllXmlParserRegisteringProcessor<>(JPAJarJBossAllParser.ROOT_ELEMENT, JpaAttachments.DEPLOYMENT_SETTINGS_KEY, new JPAJarJBossAllParser()));

                // handles parsing of persistence.xml
                processorTarget.addDeploymentProcessor(JPAExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_PERSISTENCE_UNIT, new PersistenceUnitParseProcessor(appclient));

                // handles persistence unit / context annotations in components
                processorTarget.addDeploymentProcessor(JPAExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_PERSISTENCE_ANNOTATION, new JPAAnnotationProcessor());
                // injects Jakarta Persistence dependencies into an application
                processorTarget.addDeploymentProcessor(JPAExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_JPA, new JPADependencyProcessor());
                // Inject Hibernate Search dependencies into an application
                processorTarget.addDeploymentProcessor(JPAExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES,Phase.DEPENDENCIES_HIBERNATE_SEARCH , new HibernateSearchProcessor());

                // handle ClassFileTransformer
                processorTarget.addDeploymentProcessor(JPAExtension.SUBSYSTEM_NAME, Phase.FIRST_MODULE_USE, Phase.FIRST_MODULE_USE_PERSISTENCE_CLASS_FILE_TRANSFORMER, new JPAClassFileTransformerProcessor());
                final CapabilityServiceSupport capabilities = context.getCapabilityServiceSupport();
                if (capabilities.hasCapability("org.wildfly.ejb3")) {
                    // registers listeners/interceptors on session beans
                    processorTarget.addDeploymentProcessor(JPAExtension.SUBSYSTEM_NAME, Phase.FIRST_MODULE_USE, Phase.FIRST_MODULE_USE_INTERCEPTORS, new JPAInterceptorProcessor());
                }
                // begin pu service install and deploying a persistence provider
                processorTarget.addDeploymentProcessor(JPAExtension.SUBSYSTEM_NAME, Phase.FIRST_MODULE_USE, Phase.FIRST_MODULE_USE_PERSISTENCE_PREPARE, new PersistenceBeginInstallProcessor(platform));

                // handles persistence unit / context references from deployment descriptors
                processorTarget.addDeploymentProcessor(JPAExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_PERSISTENCE_REF, new PersistenceRefProcessor());

                // handles pu deployment (completes pu service installation)
                processorTarget.addDeploymentProcessor(JPAExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_PERSISTENTUNIT, new PersistenceCompleteInstallProcessor(platform));

            }
        }, OperationContext.Stage.RUNTIME);

        final String dataSourceName = DEFAULT_DATASOURCE.resolveModelAttribute(context, model).asStringOrNull();
        final ExtendedPersistenceInheritance defaultExtendedPersistenceInheritance = ExtendedPersistenceInheritance.valueOf(DEFAULT_EXTENDEDPERSISTENCE_INHERITANCE.resolveModelAttribute(context, model).asString());

        final ServiceTarget target = context.getServiceTarget();
        JPAService.addService(target, dataSourceName, defaultExtendedPersistenceInheritance);
        JPAUserTransactionListenerService.addService(target);

    }
}
