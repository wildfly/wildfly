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
