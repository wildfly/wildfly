/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.threads;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.threads.Constants.GROUP_NAME;
import static org.jboss.as.threads.Constants.PRIORITY;
import static org.jboss.as.threads.Constants.PROPERTIES;
import static org.jboss.as.threads.Constants.THREAD_NAME_PATTERN;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelRemoveOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.server.NewRuntimeOperationContext;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class NewThreadFactoryRemove implements RuntimeOperationHandler, ModelRemoveOperationHandler {

    static final OperationHandler INSTANCE = new NewThreadFactoryRemove();

    @Override
    public Cancellable execute(final NewOperationContext context, final ModelNode operation, final ResultHandler resultHandler) {
        final ModelNode threadFactory = context.getSubModel();
        final String name = threadFactory.require(NAME).asString();

        if (context instanceof NewRuntimeOperationContext) {

            final NewRuntimeOperationContext runtimeContext = (NewRuntimeOperationContext) context;
            final ServiceController<?> controller = runtimeContext.getServiceRegistry(). getService(ThreadsServices.threadFactoryName(name));
            if (controller == null) {
                resultHandler.handleResultComplete(null);
                return Cancellable.NULL;
            } else {
                //controller.addListener(new UpdateResultHandler.ServiceRemoveListener<P>(handler, param));
                controller.setMode(Mode.REMOVE);
            }
        }


        final ModelNode compensating = new ModelNode();
        compensating.get(OP_ADDR).set(operation.require(ADDRESS));
        compensating.get(OP).set(ADD);
        compensating.get(NAME).set(name);
        compensating.get(GROUP_NAME).set(threadFactory.get(GROUP_NAME));
        compensating.get(THREAD_NAME_PATTERN).set(threadFactory.get(THREAD_NAME_PATTERN));
        compensating.get(PRIORITY).set(threadFactory.get(PRIORITY));
        compensating.get(PROPERTIES).set(threadFactory.get(PROPERTIES).clone());
        threadFactory.clear();

        resultHandler.handleResultComplete(compensating);
        return Cancellable.NULL;
    }

}
