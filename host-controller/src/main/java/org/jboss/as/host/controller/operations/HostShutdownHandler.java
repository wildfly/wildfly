/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.host.controller.operations;


import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESTART;

import java.util.Locale;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.host.controller.descriptions.HostRootDescription;
import org.jboss.as.process.ExitCodes;
import org.jboss.dmr.ModelNode;

/**
 * Stops a host.
 *
 * @author Kabir Khan
 */
public class HostShutdownHandler implements OperationStepHandler, DescriptionProvider {

    public static final String OPERATION_NAME = "shutdown";

    private final DomainController domainController;

    /**
     * Create the ServerAddHandler
     */
    public HostShutdownHandler(final DomainController domainController) {
        this.domainController = domainController;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final boolean restart = operation.hasDefined(RESTART) ? operation.get(RESTART).asBoolean() : false;
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                if (restart) {
                    //Add to the exit code so that we get respawned
                    System.exit(ExitCodes.HOST_CONTROLLER_ABORT_EXIT_CODE + 1);
                } else {
                    domainController.stopLocalHost();
                }
                context.completeStep();
            }
        }, OperationContext.Stage.RUNTIME);
        context.completeStep();
    }

    @Override
    public ModelNode getModelDescription(final Locale locale) {
        return HostRootDescription.getHostShutdownHandler(locale);
    }
}
