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

package org.jboss.as.logging.handlers.async;

import static org.jboss.as.logging.CommonAttributes.OVERFLOW_ACTION;
import static org.jboss.as.logging.CommonAttributes.QUEUE_LENGTH;
import static org.jboss.as.logging.CommonAttributes.SUBHANDLERS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Handler;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.logging.handlers.HandlerAddProperties;
import org.jboss.as.logging.util.LogServices;
import org.jboss.as.logging.util.ModelParser;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.value.InjectedValue;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Emanuel Muckenhuber
 */
public class AsyncHandlerAdd extends HandlerAddProperties<AsyncHandlerService> {

    public static final AsyncHandlerAdd INSTANCE = new AsyncHandlerAdd();

    private AsyncHandlerAdd() {
        super(Arrays.asList(SUBHANDLERS, QUEUE_LENGTH, OVERFLOW_ACTION));
    }

    @Override
    protected void updateRuntime(final OperationContext context, final ServiceBuilder<Handler> serviceBuilder, final String name, final AsyncHandlerService service, final ModelNode model, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        final List<InjectedValue<Handler>> list = new ArrayList<InjectedValue<Handler>>();
        final ModelNode subhandlers = SUBHANDLERS.resolveModelAttribute(context, model);
        if (subhandlers.isDefined()) {
            for (final ModelNode handlerName : subhandlers.asList()) {
                final InjectedValue<Handler> injectedValue = new InjectedValue<Handler>();
                serviceBuilder.addDependency(LogServices.handlerName(handlerName.asString()), Handler.class, injectedValue);
                list.add(injectedValue);
            }
        }
        service.addHandlers(list);
        service.setQueueLength(QUEUE_LENGTH.resolveModelAttribute(context, model).asInt());
        final ModelNode overflowAction = OVERFLOW_ACTION.resolveModelAttribute(context, model);
        if (overflowAction.isDefined()) {
            service.setOverflowAction(ModelParser.parseOverflowAction(overflowAction));
        }
    }

    @Override
    protected AsyncHandlerService createHandlerService(OperationContext context, final ModelNode model) throws OperationFailedException {
        return new AsyncHandlerService();
    }

}
