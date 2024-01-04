/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.subsystems.jca;

import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author <a href="jesper.pedersen@jboss.org">Jesper Pedersen</a>
 * @author <a href="stefano.maestri@redhat.com">Stefano Maestri</a>
 */
public class BeanValidationAdd extends AbstractBoottimeAddStepHandler {

    public static final BeanValidationAdd INSTANCE = new BeanValidationAdd();

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        for (JcaBeanValidationDefinition.BeanValidationParameters parameter : JcaBeanValidationDefinition.BeanValidationParameters.values() ) {
            parameter.getAttribute().validateAndSet(operation,model);
        }
    }

    @Override
    protected void performBoottime(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {

        final boolean enabled = JcaBeanValidationDefinition.BeanValidationParameters.BEAN_VALIDATION_ENABLED.getAttribute().resolveModelAttribute(context, model).asBoolean();

        ServiceName serviceName = ConnectorServices.BEAN_VALIDATION_CONFIG_SERVICE;
        ServiceName jcaConfigServiceName = ConnectorServices.CONNECTOR_CONFIG_SERVICE;

        final ServiceTarget serviceTarget = context.getServiceTarget();

        final BeanValidationService.BeanValidation config = new BeanValidationService.BeanValidation(enabled);
        final BeanValidationService service = new BeanValidationService(config);
            ServiceController<?> controller = serviceTarget.addService(serviceName, service).setInitialMode(ServiceController.Mode.ACTIVE)
                    .addDependency(jcaConfigServiceName, JcaSubsystemConfiguration.class, service.getJcaConfigInjector() )
                    .install();
    }
}
