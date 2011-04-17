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

package org.jboss.as.remoting;

import java.util.concurrent.Executor;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.NewStepHandler;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.threads.ThreadsServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.inject.CastingInjector;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceController;
import org.jboss.remoting3.Endpoint;
import org.jboss.xnio.OptionMap;

import static org.jboss.as.controller.PathAddress.EMPTY_ADDRESS;
import static org.jboss.as.controller.PathAddress.pathAddress;
import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.remoting.CommonAttributes.CONNECTOR;
import static org.jboss.as.remoting.CommonAttributes.THREAD_POOL;

/**
 * Add operation handler for the remoting subsystem.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Emanuel Muckenhuber
 */
class NewRemotingSubsystemAdd implements NewStepHandler {

    static final NewStepHandler INSTANCE = new NewRemotingSubsystemAdd();

    public void execute(final NewOperationContext context, final ModelNode operation) {
        final String threadPoolName = operation.require(THREAD_POOL).asString();
        context.writeModel(pathAddress(pathElement("hi")), null);
        context.readModel(EMPTY_ADDRESS).get(THREAD_POOL).set(threadPoolName);
        // initialize the connectors
        context.readModel(EMPTY_ADDRESS).get(CONNECTOR).setEmptyObject();
        if (context.getType() == NewOperationContext.Type.SERVER) {
            context.addStep(new NewStepHandler() {
                public void execute(final NewOperationContext context, final ModelNode operation) {
                    // create endpoint
                    final EndpointService endpointService = new EndpointService();
                    // todo configure option map
                    endpointService.setOptionMap(OptionMap.EMPTY);
                    final Injector<Executor> executorInjector = endpointService.getExecutorInjector();

                    ServiceVerificationHandler verificationHandler = new ServiceVerificationHandler();
                    final ServiceController<Endpoint> controller = context
                            .getServiceTarget()
                            .addService(RemotingServices.ENDPOINT, endpointService)
                            .addDependency(ThreadsServices.executorName(threadPoolName), new CastingInjector<Executor>(executorInjector, Executor.class))
                            .addListener(verificationHandler)
                            .install();

                    context.addStep(verificationHandler, NewOperationContext.Stage.VERIFY);

                    if (context.completeStep() == NewOperationContext.ResultAction.ROLLBACK) {
                        context.removeService(controller);
                    }
                }
            }, NewOperationContext.Stage.RUNTIME);
        }
        context.completeStep();
    }
}
