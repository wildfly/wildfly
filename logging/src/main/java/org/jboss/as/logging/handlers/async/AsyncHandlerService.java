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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.jboss.as.logging.handlers.FormatterSpec;
import org.jboss.as.logging.handlers.HandlerService;
import org.jboss.logmanager.handlers.AsyncHandler;
import org.jboss.logmanager.handlers.AsyncHandler.OverflowAction;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class AsyncHandlerService implements HandlerService {

    private final List<InjectedValue<Handler>> subhandlers = new ArrayList<InjectedValue<Handler>>();

    private OverflowAction overflowAction;

    private int queueLength;

    private AsyncHandler value;

    private Level level;
    private Filter filter;
    private FormatterSpec formatterSpec;
    private String encoding;
    private boolean autoflush;

    public synchronized void start(final StartContext context) throws StartException {
        final AsyncHandler handler = new AsyncHandler(queueLength);
        value = handler;
        formatterSpec.apply(handler);
        handler.setOverflowAction(overflowAction);
        handler.setAutoFlush(autoflush);
        if (filter != null) handler.setFilter(filter);
        try {
            handler.setEncoding(encoding);
        } catch (UnsupportedEncodingException e) {
            throw new StartException(e);
        }
        Handler[] handlers = new Handler[subhandlers.size()];
        for (int i = 0, subhandlersSize = subhandlers.size(); i < subhandlersSize; i++) {
            handlers[i] = subhandlers.get(i).getValue();
        }
        handler.setHandlers(handlers);
        if (level != null) handler.setLevel(level);
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
        final AsyncHandler handler = value;
        if (handler != null) {
            handler.setOverflowAction(overflowAction);
        }
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

    @Override
    public synchronized void setEncoding(final String encoding) throws UnsupportedEncodingException {
        this.encoding = encoding;
        final AsyncHandler handler = value;
        if (handler != null) {
            handler.setEncoding(encoding);
        }
    }

    @Override
    public synchronized void setFormatterSpec(final FormatterSpec formatterSpec) {
        this.formatterSpec = formatterSpec;
        final AsyncHandler handler = value;
        if (handler != null) {
            formatterSpec.apply(handler);
        }
    }

    @Override
    public synchronized void setFilter(final Filter filter) {
        this.filter = filter;
        final AsyncHandler handler = value;
        if (handler != null) {
            handler.setFilter(filter);
        }
    }

    public synchronized void addHandlers(final List<InjectedValue<Handler>> list) {
        subhandlers.addAll(list);
        final AsyncHandler handler = value;
        if (handler != null) {
            for (InjectedValue<Handler> injectedHandler : list) {
                handler.addHandler(injectedHandler.getValue());
            }
        }
    }

    public synchronized void addHandler(final InjectedValue<Handler> injectedHandler) {
        subhandlers.add(injectedHandler);
        final AsyncHandler handler = value;
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

            final AsyncHandler handler = value;
            if (handler != null) {
                handler.removeHandler(valueToRemove.getValue());
            }
        }
    }
}
