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
package org.jboss.as.osgi.management;

import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.Stage;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.osgi.service.FrameworkActivator;
import org.jboss.dmr.ModelNode;

/**
 * Operation to activate the OSGi subsystem.
 *
 * @author David Bosschaert
 * @author Thomas.Diesler@jboss.com
 * @author Brian.Stansberry@jboss.com
 */
public class ActivateOperationHandler extends AbstractRuntimeOnlyHandler  {

    public static ActivateOperationHandler INSTANCE = new ActivateOperationHandler();

    private ActivateOperationHandler() {
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {

        // We bypass the normal OperationContext service installation handling here by letting a ServiceTarget
        // cached previously handle service installation. But we still need to tell the context
        // this step is going to manipulate the service registry so it can acquire the exclusive operation lock and
        // also so it knows to await full MSC ServiceContainer stability before proceeding to Stage.VERIFY
        // So, ask for the service registry as a way of communicating this.
        context.getServiceRegistry(true);

        // This verification handler will track any problems associated with service controllers
        // we associate it with, making that information available in the operation response failure description.
        ServiceVerificationHandler verificationHandler = new ServiceVerificationHandler();
        if (FrameworkActivator.activateEagerly(verificationHandler)) {
            context.addStep(verificationHandler, Stage.VERIFY);
        }

        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }
}
