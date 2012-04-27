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

package org.jboss.as.logging.handlers;

import java.io.UnsupportedEncodingException;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Date: 23.09.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public abstract class HandlerService<T extends Handler> implements Service<T> {

    private FormatterSpec formatterSpec;
    private Level level;
    private String encoding;
    private Filter filter;
    private T value;

    @Override
    public final synchronized void start(final StartContext context) throws StartException {
        final T handler = createHandler();
        value = handler;
        getFormatterSpec().apply(handler);
        final Filter filter = getFilter();
        if (filter != null) handler.setFilter(filter);
        try {
            handler.setEncoding(getEncoding());
        } catch (UnsupportedEncodingException e) {
            throw new StartException(e);
        }
        final Level level = getLevel();
        if (level != null) handler.setLevel(level);
        start(context, handler);
    }

    @Override
    public final synchronized void stop(final StopContext context) {
        final T handler = value;
        handler.close();
        handler.setLevel(Level.OFF);
        stop(context, handler);
        value = null;
    }

    @Override
    public final synchronized T getValue() {
        return value;
    }

    /**
     * Creates the handler.
     *
     * @return the handler
     */
    protected abstract T createHandler() throws StartException;

    /**
     * Starts the handler service.
     *
     * @param context the start context.
     * @param handler the handler.
     */
    protected abstract void start(StartContext context, T handler) throws StartException;

    /**
     * Stops the handler service.
     *
     * @param context the start context.
     * @param handler the handler.
     */
    protected void stop(StopContext context, T handler) {
        // no-op
    }

    /**
     * Returns the level the handler is set to.
     *
     * @return the log level.
     */
    public final synchronized Level getLevel() {
        return level;
    }

    public final synchronized void setLevel(final Level level) {
        this.level = level;
        final T handler = getValue();
        if (handler != null) {
            handler.setLevel(level);
        }
    }

    public final synchronized String getEncoding() {
        return encoding;
    }

    public final synchronized void setEncoding(String encoding) throws UnsupportedEncodingException {
        this.encoding = encoding;
        final T handler = getValue();
        if (handler != null) {
            handler.setEncoding(encoding);
        }
    }

    public final synchronized FormatterSpec getFormatterSpec() {
        return formatterSpec;
    }

    public final synchronized void setFormatterSpec(FormatterSpec formatterSpec) {
        this.formatterSpec = formatterSpec;
        final T handler = getValue();
        if (handler != null && formatterSpec != null) {
            formatterSpec.apply(handler);
        }
    }

    public final synchronized Filter getFilter() {
        return filter;
    }

    public final synchronized void setFilter(Filter filter) {
        this.filter = filter;
        final T handler = getValue();
        if (handler != null) {
            handler.setFilter(filter);
        }
    }
}
