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
package org.jboss.as.connector.subsystems.jca;

import java.util.List;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author <a href="jesper.pedersen@jboss.org">Jesper Pedersen</a>
 * @author <a href="stefano.maestri@redhat.com">Stefano Maestri</a>
 */
public class ArchiveValidationAdd extends AbstractBoottimeAddStepHandler {

    public static final ArchiveValidationAdd INSTANCE = new ArchiveValidationAdd();

    public static enum ArchiveValidationParameters {
        ARCHIVE_VALIDATION_ENABLED(SimpleAttributeDefinitionBuilder.create("enabled", ModelType.BOOLEAN)
            .setAllowExpression(true)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode().set(true))
            .setMeasurementUnit(MeasurementUnit.NONE)
            .setRestartAllServices()
            .setXmlName("enabled")
            .build()),
        ARCHIVE_VALIDATION_FAIL_ON_ERROR(SimpleAttributeDefinitionBuilder.create("fail-on-error", ModelType.BOOLEAN)
            .setAllowExpression(true)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode().set(true))
            .setMeasurementUnit(MeasurementUnit.NONE)
            .setRestartAllServices()
            .setXmlName("fail-on-error")
            .build()),
        ARCHIVE_VALIDATION_FAIL_ON_WARN(SimpleAttributeDefinitionBuilder.create("fail-on-warn", ModelType.BOOLEAN)
            .setAllowExpression(true)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode().set(false))
            .setMeasurementUnit(MeasurementUnit.NONE)
            .setRestartAllServices()
            .setXmlName("fail-on-warn")
            .build());

        private ArchiveValidationParameters(SimpleAttributeDefinition attribute) {
            this.attribute = attribute;
        }

        public SimpleAttributeDefinition getAttribute() {
            return attribute;
        }

        private SimpleAttributeDefinition attribute;
    }

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        for (ArchiveValidationParameters parameter : ArchiveValidationParameters.values() ) {
            parameter.getAttribute().validateAndSet(operation,model);
        }
    }

    @Override
    protected void performBoottime(final OperationContext context, final ModelNode operation, final ModelNode model,
            final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {

        final boolean enabled = ArchiveValidationParameters.ARCHIVE_VALIDATION_ENABLED.getAttribute().validateResolvedOperation(model).asBoolean();
        final boolean failOnError = ArchiveValidationParameters.ARCHIVE_VALIDATION_FAIL_ON_ERROR.getAttribute().validateResolvedOperation(model).asBoolean();
        final boolean failOnWarn = ArchiveValidationParameters.ARCHIVE_VALIDATION_FAIL_ON_WARN.getAttribute().validateResolvedOperation(model).asBoolean();

        ServiceName serviceName = ConnectorServices.ARCHIVE_VALIDATION_CONFIG_SERVICE;
        ServiceName jcaConfigServiceName = ConnectorServices.CONNECTOR_CONFIG_SERVICE;

        final ServiceTarget serviceTarget = context.getServiceTarget();

        final ArchiveValidationService.ArchiveValidation config = new ArchiveValidationService.ArchiveValidation(enabled, failOnError, failOnWarn);
        final ArchiveValidationService service = new ArchiveValidationService(config);
            ServiceController<?> controller = serviceTarget.addService(serviceName, service).setInitialMode(ServiceController.Mode.ACTIVE)
                    .addDependency(jcaConfigServiceName, JcaSubsystemConfiguration.class, service.getJcaConfigInjector() )
                    .addListener(verificationHandler).install();

        context.addStep(verificationHandler, OperationContext.Stage.VERIFY);

    }
}
