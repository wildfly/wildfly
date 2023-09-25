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
public class ArchiveValidationAdd extends AbstractBoottimeAddStepHandler {

    public static final ArchiveValidationAdd INSTANCE = new ArchiveValidationAdd();

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        for (JcaArchiveValidationDefinition.ArchiveValidationParameters parameter : JcaArchiveValidationDefinition.ArchiveValidationParameters.values() ) {
            parameter.getAttribute().validateAndSet(operation,model);
        }
    }

    @Override
    protected void performBoottime(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {

        final boolean enabled = JcaArchiveValidationDefinition.ArchiveValidationParameters.ARCHIVE_VALIDATION_ENABLED.getAttribute().resolveModelAttribute(context, model).asBoolean();
        final boolean failOnError = JcaArchiveValidationDefinition.ArchiveValidationParameters.ARCHIVE_VALIDATION_FAIL_ON_ERROR.getAttribute().resolveModelAttribute(context, model).asBoolean();
        final boolean failOnWarn = JcaArchiveValidationDefinition.ArchiveValidationParameters.ARCHIVE_VALIDATION_FAIL_ON_WARN.getAttribute().resolveModelAttribute(context, model).asBoolean();

        ServiceName serviceName = ConnectorServices.ARCHIVE_VALIDATION_CONFIG_SERVICE;
        ServiceName jcaConfigServiceName = ConnectorServices.CONNECTOR_CONFIG_SERVICE;

        final ServiceTarget serviceTarget = context.getServiceTarget();

        final ArchiveValidationService.ArchiveValidation config = new ArchiveValidationService.ArchiveValidation(enabled, failOnError, failOnWarn);
        final ArchiveValidationService service = new ArchiveValidationService(config);
            ServiceController<?> controller = serviceTarget.addService(serviceName, service).setInitialMode(ServiceController.Mode.ACTIVE)
                    .addDependency(jcaConfigServiceName, JcaSubsystemConfiguration.class, service.getJcaConfigInjector() )
                    .install();
    }
}
