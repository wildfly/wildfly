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

import java.util.List;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.jpa.persistenceprovider.PersistenceProviderResolverImpl;
import org.jboss.as.jpa.processor.JPAAnnotationParseProcessor;
import org.jboss.as.jpa.processor.JPAClassFileTransformerProcessor;
import org.jboss.as.jpa.processor.JPADependencyProcessor;
import org.jboss.as.jpa.processor.JPAInterceptorProcessor;
import org.jboss.as.jpa.processor.PersistenceProviderProcessor;
import org.jboss.as.jpa.processor.PersistenceRefProcessor;
import org.jboss.as.jpa.processor.PersistenceUnitDeploymentProcessor;
import org.jboss.as.jpa.processor.PersistenceUnitParseProcessor;
import org.jboss.as.jpa.service.JPAService;
import org.jboss.as.jpa.service.JPAUserTransactionListenerService;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;


/**
 * Add the JPA subsystem directive.
 * <p/>
 *
 * @author Scott Marlow
 */

class JPASubSystemAdd extends AbstractBoottimeAddStepHandler {

    public static JPASubSystemAdd INSTANCE = new JPASubSystemAdd();

    private ParametersValidator modelValidator = new ParametersValidator();
    private ParametersValidator runtimeValidator = new ParametersValidator();

    private JPASubSystemAdd() {
        modelValidator.registerValidator(CommonAttributes.DEFAULT_DATASOURCE, new StringLengthValidator(0, Integer.MAX_VALUE, true, true));
        runtimeValidator.registerValidator(CommonAttributes.DEFAULT_DATASOURCE, new StringLengthValidator(0, Integer.MAX_VALUE, true, false));
    }

    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        JPADefinition.DEFAULT_DATASOURCE.validateAndSet(operation, model);
    }

    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws
        OperationFailedException {

        runtimeValidator.validate(operation.resolve());
        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {

                // set Hibernate persistence provider as the default provider
                javax.persistence.spi.PersistenceProviderResolverHolder.setPersistenceProviderResolver(
                    PersistenceProviderResolverImpl.getInstance());

                // handles parsing of persistence.xml
                processorTarget.addDeploymentProcessor(JPAExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_PERSISTENCE_UNIT, new PersistenceUnitParseProcessor());
                // handles persistence unit / context annotations in components
                processorTarget.addDeploymentProcessor(JPAExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_PERSISTENCE_ANNOTATION, new JPAAnnotationParseProcessor());
                // injects JPA dependencies into an application
                processorTarget.addDeploymentProcessor(JPAExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_JPA, new JPADependencyProcessor());
                // handles persistence unit / context references from deployment descriptors
                processorTarget.addDeploymentProcessor(JPAExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_PERSISTENCE_REF, new PersistenceRefProcessor());
                // handle ClassFileTransformer
                processorTarget.addDeploymentProcessor(JPAExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_PERSISTENCE_CLASS_FILE_TRANSFORMER, new JPAClassFileTransformerProcessor());
                // registers listeners/interceptors on session beans
                processorTarget.addDeploymentProcessor(JPAExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_JPA_INTERCEPTORS, new JPAInterceptorProcessor());
                // handles deploying a persistence provider
                processorTarget.addDeploymentProcessor(JPAExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_PERSISTENCE_PROVIDER, new PersistenceProviderProcessor());
                // handles pu deployment (starts pu service)
                processorTarget.addDeploymentProcessor(JPAExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_PERSISTENTUNIT, new PersistenceUnitDeploymentProcessor(PersistenceUnitRegistryImpl.INSTANCE));
            }
        }, OperationContext.Stage.RUNTIME);

        final ModelNode defaultDSNode = operation.require(CommonAttributes.DEFAULT_DATASOURCE);
        final String dataSourceName = defaultDSNode.resolve().asString();
        final ServiceTarget target = context.getServiceTarget();
        newControllers.add(JPAService.addService(target, dataSourceName, verificationHandler));
        newControllers.add(JPAUserTransactionListenerService.addService(target, verificationHandler));
    }
}
