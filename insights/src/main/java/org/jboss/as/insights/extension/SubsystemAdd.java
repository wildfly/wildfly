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
package org.jboss.as.insights.extension;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.jdr.JdrReportCollector;
import org.jboss.as.jdr.JdrReportService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceController.Mode;

/**
 * Handler responsible for adding the subsystem resource to the model
 *
 * @author <a href="jkinlaw@jboss.com">Josh Kinlaw</a>
 */
class SubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final SubsystemAdd INSTANCE = new SubsystemAdd();

    private SubsystemAdd() {
    }

    /** {@inheritDoc} */
    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        InsightsSubsystemDefinition.FREQUENCY.validateAndSet(operation, model);
        InsightsSubsystemDefinition.ENABLED.validateAndSet(operation, model);
        InsightsSubsystemDefinition.RHNUID.validateAndSet(operation, model);
        InsightsSubsystemDefinition.RHNPW.validateAndSet(operation, model);
        InsightsSubsystemDefinition.PROXYUSER.validateAndSet(operation, model);
        InsightsSubsystemDefinition.PROXYPASSWORD.validateAndSet(operation, model);
        InsightsSubsystemDefinition.PROXYPORT.validateAndSet(operation, model);
        InsightsSubsystemDefinition.PROXYURL.validateAndSet(operation, model);
    }

    /** {@inheritDoc} */
    @Override
    public void performBoottime(OperationContext context, ModelNode operation, ModelNode model)
            throws OperationFailedException {

        long frequency = InsightsSubsystemDefinition.FREQUENCY.resolveModelAttribute(context, model).asLong();
        boolean enabled = InsightsSubsystemDefinition.ENABLED.resolveModelAttribute(context, model).asBoolean();
        InsightsService service = InsightsService.getInstance(frequency, enabled);
        ServiceName name = InsightsService.createServiceName();
        ServiceRegistry registry = context.getServiceRegistry(false);
        JdrReportCollector jdrCollector = JdrReportCollector.class.cast(registry.getRequiredService(JdrReportService.SERVICE_NAME).getValue());
        service.setJdrReportCollector(jdrCollector);
        context.getServiceTarget()
                .addService(name, service)
                .setInitialMode(Mode.ACTIVE)
                .install();
    }
}
