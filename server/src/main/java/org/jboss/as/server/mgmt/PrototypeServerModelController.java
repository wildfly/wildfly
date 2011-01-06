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
package org.jboss.as.server.mgmt;

import org.jboss.as.controller.AbstractModelController;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ResultHandler;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * This is just here until we replace it with something proper
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class PrototypeServerModelController extends AbstractModelController implements Service<PrototypeServerModelController>{

    public static final ServiceName SERVICE_NAME = Services.JBOSS_AS.append("server", "model", "controller");

    @Override
    public void registerOperationHandler(PathAddress address, String name, OperationHandler handler) {
    }

    @Override
    public Operation execute(final ModelNode operation, final ResultHandler handler) {
        //Just a silly echo thing handled for now
        String operationName = operation.get("operation").asString();
        if (!"echo".equals(operationName)) {
            throw new RuntimeException("Unknown operation name " + operationName);
        }

        ModelNode sleepNode = operation.get("sleep");
        final long sleep = sleepNode.isDefined() ? sleepNode.asLong() : 0;
        if (sleep > 0) {
            final Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException e) {
                        return;
                    }
                    handler.handleResultFragment(new String[] {"asynch-result"}, operation);
                    try {
                        Thread.sleep(sleep * 2);
                    } catch (InterruptedException e) {
                        System.out.println("!!! Interrupted");
                        return;
                    }
                    handler.handleResultComplete();
                }
            });
            t.start();

            return new ModelController.Operation() {

                @Override
                public void cancel() {
                    t.interrupt();
                    handler.handleCancellation();
                }
            };

        } else {
            handler.handleResultFragment(new String[] {"result"}, operation);
            handler.handleResultComplete();
            return new ModelController.Operation() {

                @Override
                public void cancel() {
                }
            };
        }
    }

    @Override
    public PrototypeServerModelController getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public void start(StartContext context) throws StartException {
    }

    @Override
    public void stop(StopContext context) {
    }
}
