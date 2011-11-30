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

import java.util.Locale;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.RunningModeControl;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.descriptions.DefaultOperationDescriptionProvider;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.server.Services;
import org.jboss.as.server.controller.descriptions.ServerDescriptions;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;

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

    private final RunningModeControl runningModeControl;
    private DescriptionProvider descriptionProvider;

    private final AttributeDefinition adminOnlyAttribute = new SimpleAttributeDefinition(ModelDescriptionConstants.ADMIN_ONLY, ModelType.BOOLEAN, true);

    public ServerReloadHandler(RunningModeControl runningModeControl) {
        this.runningModeControl = runningModeControl;
    }

    /** {@inheritDoc} */
    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                final boolean adminOnly = adminOnlyAttribute.resolveModelAttribute(context, operation).asBoolean(false);
                final ServiceController<?> service = context.getServiceRegistry(true).getRequiredService(Services.JBOSS_AS);
                if(context.completeStep() == OperationContext.ResultAction.KEEP) {
                    service.addListener(new AbstractServiceListener<Object>() {
                        public void listenerAdded(final ServiceController<?> controller) {
                            controller.setMode(ServiceController.Mode.NEVER);
                        }

                        public void transition(final ServiceController<? extends Object> controller, final ServiceController.Transition transition) {
                            if (transition == ServiceController.Transition.STOPPING_to_DOWN) {
                                controller.removeListener(this);
                                runningModeControl.setRunningMode(adminOnly ? RunningMode.ADMIN_ONLY : RunningMode.NORMAL);
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
        return getDescriptionProvider().getModelDescription(locale);
    }

    private synchronized DescriptionProvider getDescriptionProvider() {
        if (descriptionProvider == null) {
            descriptionProvider = new DefaultOperationDescriptionProvider(OPERATION_NAME, ServerDescriptions.getResourceDescriptionResolver("server"), adminOnlyAttribute);
        }
        return descriptionProvider;
    }
}
