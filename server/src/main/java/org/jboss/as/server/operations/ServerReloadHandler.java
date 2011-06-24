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

package org.jboss.as.server.operations;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.server.Services;
import org.jboss.as.server.controller.descriptions.ServerDescriptionProviders;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;

import java.util.Collections;
import java.util.Locale;

/**
 * Server-reload operation handler.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class ServerReloadHandler implements OperationStepHandler, DescriptionProvider {

    /**
     * The operation name.
     */
    public static final String OPERATION_NAME = "reload";
    /**
     * The operation instance.
     */
    public static final ServerReloadHandler INSTANCE = new ServerReloadHandler();

    private ServerReloadHandler() {
    }

    /** {@inheritDoc} */
    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                final ServiceController<?> service = context.getServiceRegistry(true).getRequiredService(Services.JBOSS_AS);
                context.reloadRequired();
                if(context.completeStep() == OperationContext.ResultAction.KEEP) {
                    service.addListener(new AbstractServiceListener<Object>() {
                        public void listenerAdded(final ServiceController<?> controller) {
                            controller.setMode(ServiceController.Mode.NEVER);
                        }

                        public void transition(final ServiceController<? extends Object> controller, final ServiceController.Transition transition) {
                            if (transition == ServiceController.Transition.STOPPING_to_DOWN) {
                                controller.removeListener(this);
                                controller.setMode(ServiceController.Mode.ACTIVE);
                            }
                        }
                    });
                }
            }
        }, OperationContext.Stage.RUNTIME);
        context.completeStep();
    }

    /** {@inheritDoc} */
    public ModelNode getModelDescription(final Locale locale) {
        return ServerDescriptionProviders.RELOAD_PROVIDER.getModelDescription(locale);
    }
}
