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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;

import java.util.List;
import java.util.Locale;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.jpa.hibernate.HibernatePersistenceProviderAdaptor;
import org.jboss.as.jpa.persistenceprovider.PersistenceProviderAdapterRegistry;
import org.jboss.as.jpa.persistenceprovider.PersistenceProviderResolverImpl;
import org.jboss.as.jpa.processor.JPAAnnotationParseProcessor;
import org.jboss.as.jpa.processor.JPADependencyProcessor;
import org.jboss.as.jpa.processor.PersistenceRefProcessor;
import org.jboss.as.jpa.processor.PersistenceUnitDeploymentProcessor;
import org.jboss.as.jpa.processor.PersistenceUnitParseProcessor;
import org.jboss.as.jpa.service.JPAService;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * Add the JPA subsystem directive.
 * <p/>
 * TODO:  add subsystem configuration properties
 *
 * @author Scott Marlow
 */

class JPASubSystemAdd extends AbstractBoottimeAddStepHandler implements DescriptionProvider {


    static ModelNode getAddOperation(ModelNode address, ModelNode currentModel) {
        ModelNode addOp = Util.getEmptyOperation(OPERATION_NAME, address);
        addOp.get(CommonAttributes.DEFAULT_DATASOURCE).set(currentModel.get(CommonAttributes.DEFAULT_DATASOURCE));
        return addOp;
    }

    static final String OPERATION_NAME = ADD;
    static final JPASubSystemAdd INSTANCE = new JPASubSystemAdd();

    private ParametersValidator modelValidator = new ParametersValidator();
    private ParametersValidator runtimeValidator = new ParametersValidator();

    private JPASubSystemAdd() {
        modelValidator.registerValidator(CommonAttributes.DEFAULT_DATASOURCE, new StringLengthValidator(0, Integer.MAX_VALUE, false, true));
        runtimeValidator.registerValidator(CommonAttributes.DEFAULT_DATASOURCE, new StringLengthValidator(0, Integer.MAX_VALUE, false, false));
    }


    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        modelValidator.validate(operation);
        final ModelNode defaultDSNode = operation.require(CommonAttributes.DEFAULT_DATASOURCE);
        model.get(CommonAttributes.DEFAULT_DATASOURCE).set(defaultDSNode);
    }

    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {

        runtimeValidator.validate(operation.resolve());

        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {

                /* set Hibernate persistence provider as the default provider */
                javax.persistence.spi.PersistenceProviderResolverHolder.setPersistenceProviderResolver(
                        PersistenceProviderResolverImpl.getInstance());

                PersistenceProviderAdapterRegistry.putPersistenceProviderAdaptor(
                        "org.hibernate.ejb.HibernatePersistence", new HibernatePersistenceProviderAdaptor());

                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_PERSISTENCE_UNIT, new PersistenceUnitParseProcessor());
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_PERSISTENCE_ANNOTATION, new JPAAnnotationParseProcessor());
                processorTarget.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_JPA, new JPADependencyProcessor());
                // TODO: enable updateContext.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_PERSISTENCE_PROVIDER, new PersistenceProviderProcessor());
                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_PERSISTENCE_REF, new PersistenceRefProcessor());
                processorTarget.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_PERSISTENTUNIT, new PersistenceUnitDeploymentProcessor());
            }
        }, OperationContext.Stage.RUNTIME);

        final ModelNode defaultDSNode = operation.require(CommonAttributes.DEFAULT_DATASOURCE);
        final String dataSourceName = defaultDSNode.resolve().asString();
        final ServiceTarget target = context.getServiceTarget();
        newControllers.add(JPAService.addService(target, dataSourceName, verificationHandler));

    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return JPADescriptions.getSubsystemAdd(locale);
    }

}
