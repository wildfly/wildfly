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

package org.jboss.as.messaging;

import org.hornetq.api.core.management.HornetQServerControl;
import org.hornetq.core.server.HornetQServer;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * Collection of messaging metric handlers.
 *
 * @author Emanuel Muckenhuber
 */
class MessagingMetricHandlers {

    static final String CONNECTION_COUNT = "connection-count";
    static final String LIVE_CONNECTOR_NAME = "live-connector-name";

    static void registerMetrics(final ManagementResourceRegistration registration) {
        registration.registerMetric(LIVE_CONNECTOR_NAME, new AbstractMessagingOperationHandler() {
            @Override
            protected void process(OperationContext context, ModelNode operation, HornetQServerControl control) {
                try {
                    context.getResult().set(control.getLiveConnectorName());
                } catch(Exception e) {
                    context.getFailureDescription().set("failed to get live connector name", e.getMessage());
                }
            }
        });
        registration.registerMetric(CONNECTION_COUNT, new AbstractMessagingOperationHandler() {
            @Override
            protected void process(OperationContext context, ModelNode operation, HornetQServerControl control) {
                context.getResult().set(control.getConnectionCount());
            }
        });
        // TODO more runtime operations exposed by HornetQServerControl
    }

    private abstract static class AbstractMessagingOperationHandler implements OperationStepHandler {
        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                    final ServiceController<?> controller = context.getServiceRegistry(false).getService(MessagingServices.JBOSS_MESSAGING);
                    if(controller != null) {
                        final HornetQServer server = HornetQServer.class.cast(controller.getValue());
                        process(context, operation, server);
                    }
                    context.completeStep();
                }
            }, OperationContext.Stage.RUNTIME);
            context.completeStep();
        }

        protected void process(final OperationContext context, final ModelNode operation, final HornetQServer server) {
            process(context, operation, server.getHornetQServerControl());
        }

        protected void process(final OperationContext context, final ModelNode operation, final HornetQServerControl control) {
            //
        }
    }

}
