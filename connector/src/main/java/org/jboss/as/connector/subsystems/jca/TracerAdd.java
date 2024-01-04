/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.subsystems.jca;

import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.controller.AbstractAddStepHandler;
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
public class TracerAdd extends AbstractAddStepHandler {

    public static final TracerAdd INSTANCE = new TracerAdd();

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        for (TracerDefinition.TracerParameters parameter : TracerDefinition.TracerParameters.values()) {
            parameter.getAttribute().validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {

        final boolean enabled = TracerDefinition.TracerParameters.TRACER_ENABLED.getAttribute().resolveModelAttribute(context, model).asBoolean();

        ServiceName serviceName = ConnectorServices.TRACER_CONFIG_SERVICE;
        ServiceName jcaConfigServiceName = ConnectorServices.CONNECTOR_CONFIG_SERVICE;

        final ServiceTarget serviceTarget = context.getServiceTarget();

        final TracerService.Tracer config = new TracerService.Tracer(enabled);
        final TracerService service = new TracerService(config);
        serviceTarget.addService(serviceName, service).setInitialMode(ServiceController.Mode.ACTIVE)
                .addDependency(jcaConfigServiceName, JcaSubsystemConfiguration.class, service.getJcaConfigInjector())
                .install();

    }
}
