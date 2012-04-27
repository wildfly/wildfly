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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;

import org.jboss.as.logging.handlers.FlushingHandlerService;
import org.jboss.logmanager.handlers.AsyncHandler;
import org.jboss.logmanager.handlers.AsyncHandler.OverflowAction;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class AsyncHandlerService extends FlushingHandlerService<AsyncHandler> {

    private final List<InjectedValue<Handler>> subhandlers = new ArrayList<InjectedValue<Handler>>();

    private OverflowAction overflowAction;

    private int queueLength;

    @Override
    protected AsyncHandler createHandler() {
        return new AsyncHandler(queueLength);
    }

    @Override
    protected void start(final StartContext context, final AsyncHandler handler) throws StartException {
        handler.setOverflowAction(overflowAction);
        handler.setAutoFlush(isAutoflush());
        Handler[] handlers = new Handler[subhandlers.size()];
        for (int i = 0, subhandlersSize = subhandlers.size(); i < subhandlersSize; i++) {
            handlers[i] = subhandlers.get(i).getValue();
        }
        handler.setHandlers(handlers);
    }

    @Override
    protected void stop(final StopContext context, final AsyncHandler handler) {
        handler.clearHandlers();
    }

    public synchronized void setOverflowAction(final OverflowAction overflowAction) {
        this.overflowAction = overflowAction;
        final AsyncHandler handler = getValue();
        if (handler != null) {
            handler.setOverflowAction(overflowAction);
        }
    }

    public synchronized void setQueueLength(final int queueLength) {
        this.queueLength = queueLength;
    }

    public synchronized void addHandlers(final List<InjectedValue<Handler>> list) {
        subhandlers.addAll(list);
        final AsyncHandler handler = getValue();
        if (handler != null) {
            for (InjectedValue<Handler> injectedHandler : list) {
                handler.addHandler(injectedHandler.getValue());
            }
        }
    }

    public synchronized void addHandler(final InjectedValue<Handler> injectedHandler) {
        subhandlers.add(injectedHandler);
        final AsyncHandler handler = getValue();
        if (handler != null) {
            handler.addHandler(injectedHandler.getValue());
        }
    }

    public synchronized void removeHandler(final Handler subHandler) {
        InjectedValue<Handler> valueToRemove = null;
        for (InjectedValue<Handler> injectedHandler : subhandlers) {
            if (injectedHandler.getValue().equals(subHandler)) valueToRemove = injectedHandler;
        }
        if (valueToRemove != null) {

            subhandlers.remove(valueToRemove);

            final AsyncHandler handler = getValue();
            if (handler != null) {
                handler.removeHandler(valueToRemove.getValue());
            }
        }
    }
}
