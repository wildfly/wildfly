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

package org.jboss.as.logging;

import java.util.ArrayList;
import java.util.List;
import org.jboss.logmanager.handlers.AsyncHandler;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import java.util.logging.Handler;
import java.util.logging.Level;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class AsyncHandlerService implements Service<Handler> {

    private final List<InjectedValue<Handler>> subhandlers = new ArrayList<InjectedValue<Handler>>();

    private OverflowAction overflowAction;

    private int queueLength;

    private AsyncHandler value;

    private Level level;

    public synchronized void start(final StartContext context) throws StartException {
        final AsyncHandler handler = new AsyncHandler(queueLength);
        value = handler;
        final OverflowAction action = overflowAction;
        setAction(handler, action);
        Handler[] handlers = new Handler[subhandlers.size()];
        for (int i = 0, subhandlersSize = subhandlers.size(); i < subhandlersSize; i++) {
            handlers[i] = subhandlers.get(i).getValue();
        }
        handler.setHandlers(handlers);
        if (level != null) handler.setLevel(level);
    }

    private static void setAction(final AsyncHandler handler, final OverflowAction action) {
        if (handler == null) {
            return;
        }
        switch (action) {
            case BLOCK: {
                handler.setOverflowAction(AsyncHandler.OverflowAction.BLOCK);
                break;
            }
            case DISCARD: {
                handler.setOverflowAction(AsyncHandler.OverflowAction.DISCARD);
                break;
            }
        }
    }

    public synchronized void stop(final StopContext context) {
        final AsyncHandler handler = value;
        handler.close();
        handler.setLevel(Level.OFF);
        handler.clearHandlers();
        value = null;
    }

    public synchronized Handler getValue() throws IllegalStateException {
        return value;
    }

    public synchronized void setOverflowAction(final OverflowAction overflowAction) {
        this.overflowAction = overflowAction;
        setAction(value, overflowAction);
    }

    public synchronized void setQueueLength(final int queueLength) {
        this.queueLength = queueLength;
    }

    public synchronized void setLevel(final Level level) {
        this.level = level;
        final AsyncHandler handler = value;
        if (handler != null) {
            handler.setLevel(level);
        }
    }

    public synchronized void addHandlers(final List<InjectedValue<Handler>> list) {
        subhandlers.addAll(list);
    }
}
