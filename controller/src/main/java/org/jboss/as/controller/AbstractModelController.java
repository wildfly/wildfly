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

package org.jboss.as.controller;

import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractModelController implements ModelController {
    private final OperationRegistry registry = OperationRegistry.create();

    public void registerOperationHandler(final PathAddress address, final String name, final OperationHandler handler) {
        registry.register(address, name, handler);
    }

    protected OperationHandler getHandler(final PathAddress address, final String name) {
        return registry.getHandler(address, name);
    }

    public ModelNode execute(final ModelNode operation) {
        final AtomicInteger status = new AtomicInteger();
        final ModelNode finalResult = new ModelNode();
        final Operation handle = execute(operation, new ResultHandler() {
            public void handleResultFragment(final String[] location, final ModelNode result) {
                synchronized (finalResult) {
                    finalResult.get(location).set(result);
                }
            }

            public void handleResultComplete() {
                synchronized (finalResult) {
                    status.set(1);
                    finalResult.notify();
                }
            }

            public void handleCancellation() {
                synchronized (finalResult) {
                    status.set(2);
                    finalResult.notify();
                }
            }
        });
        boolean intr = false;
        try {
            synchronized (finalResult) {
                for (;;) {
                    try {
                        final int s = status.get();
                        switch (s) {
                            case 1: return finalResult;
                            case 2: throw new CancellationException();
                        }
                        finalResult.wait();
                    } catch (InterruptedException e) {
                        intr = true;
                        handle.cancel();
                    }
                }
            }
        } finally {
            if (intr) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
