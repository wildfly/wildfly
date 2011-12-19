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

package org.jboss.as.cmp.subsystem;

import java.util.List;
import java.util.Locale;

import org.jboss.as.cmp.component.CmpEntityBeanComponentDescription;
import org.jboss.as.cmp.keygenerator.KeyGeneratorFactoryRegistry;
import org.jboss.as.cmp.keygenerator.uuid.UUIDKeyGeneratorFactory;
import org.jboss.as.cmp.processors.CmpDependencyProcessor;
import org.jboss.as.cmp.processors.CmpEntityBeanComponentDescriptionFactory;
import org.jboss.as.cmp.processors.CmpEntityMetaDataProcessor;
import org.jboss.as.cmp.processors.CmpParsingProcessor;
import org.jboss.as.cmp.processors.CmpStoreManagerProcessor;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController;

/**
 * @author John Bailey
 */
public class CmpSubsystemAdd extends AbstractBoottimeAddStepHandler implements DescriptionProvider {

    private static final Logger logger = Logger.getLogger(CmpSubsystemAdd.class);
    static CmpSubsystemAdd INSTANCE = new CmpSubsystemAdd();

    protected void performBoottime(final OperationContext context, final ModelNode operation, final ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        logger.info("Activating EJB CMP Subsystem");
        final boolean appclient = context.getProcessType() == ProcessType.APPLICATION_CLIENT;

        final KeyGeneratorFactoryRegistry keyGeneratorFactoryRegistry = new KeyGeneratorFactoryRegistry();
        newControllers.add(context.getServiceTarget().addService(KeyGeneratorFactoryRegistry.SERVICE_NAME, keyGeneratorFactoryRegistry)
            .addListener(verificationHandler)
            .install());

        final UUIDKeyGeneratorFactory uuidKeyGeneratorFactory = new UUIDKeyGeneratorFactory();
        newControllers.add(context.getServiceTarget().addService(UUIDKeyGeneratorFactory.SERVICE_NAME, uuidKeyGeneratorFactory)
            .addDependency(KeyGeneratorFactoryRegistry.SERVICE_NAME, KeyGeneratorFactoryRegistry.class, KeyGeneratorFactoryRegistry.getRegistryInjector(UUIDKeyGeneratorFactory.class.getSimpleName(), uuidKeyGeneratorFactory))
            .addListener(verificationHandler)
            .install());

        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {

                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_CMP_ENTITY_BEAN_CREATE_COMPONENT_DESCRIPTIONS, new CmpEntityBeanComponentDescriptionFactory(appclient));
                if (!appclient) {
                    processorTarget.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_CMP, new CmpDependencyProcessor());
                    processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_CMP_PARSE, new CmpParsingProcessor());
                    processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_CMP_ENTITY_METADATA, new CmpEntityMetaDataProcessor(CmpEntityBeanComponentDescription.class));
                    processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_CMP_STORE_MANAGER, new CmpStoreManagerProcessor());
                }
            }
        }, OperationContext.Stage.RUNTIME);


    }

    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {

    }

    public ModelNode getModelDescription(final Locale locale) {
        return CmpSubsystemDescriptions.getSubystemAddDescription(locale);
    }
}
