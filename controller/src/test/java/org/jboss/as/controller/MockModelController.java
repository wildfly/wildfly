/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import java.util.concurrent.Executor;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.client.OperationResponse;
import org.jboss.as.controller.registry.NotificationHandlerRegistration;
import org.jboss.dmr.ModelNode;

/**
 * Base class for mock ModelController impls used in tests.
 *
 * @author Brian Stansberry (c) 2014 Red Hat Inc.
 */
public abstract class MockModelController implements ModelController {

    @Override
    public OperationResponse execute(Operation operation, OperationMessageHandler handler, OperationTransactionControl control) {
        ModelNode simpleResponse = execute(operation.getOperation(), handler, control, operation);
        return OperationResponse.Factory.createSimple(simpleResponse);
    }

    @Override
    public ModelControllerClient createClient(Executor executor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotificationHandlerRegistration getNotificationRegistry() {
        throw new UnsupportedOperationException();
    }
}
