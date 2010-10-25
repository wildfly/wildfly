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

import java.io.UnsupportedEncodingException;
import org.jboss.logmanager.handlers.ConsoleHandler;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import java.util.logging.Handler;
import java.util.logging.Level;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ConsoleHandlerService implements Service<Handler> {

    private AbstractFormatterSpec formatterSpec;

    private Level level;

    private Target target;

    private boolean autoflush;

    private String encoding;

    private ConsoleHandler value;

    public synchronized void start(final StartContext context) throws StartException {
        final ConsoleHandler handler = new ConsoleHandler();
        value = handler;
        formatterSpec.apply(handler);
        setTarget(handler, target);
        if (level != null) handler.setLevel(level);
        handler.setAutoFlush(autoflush);
        try {
            handler.setEncoding(encoding);
        } catch (UnsupportedEncodingException e) {
            throw new StartException(e);
        }
    }

    private static void setTarget(final ConsoleHandler handler, final Target target) {
        if (handler == null || target == null) return;
        switch (target) {
            case SYSTEM_ERR: {
                handler.setTarget(ConsoleHandler.Target.SYSTEM_ERR);
                break;
            }
            case SYSTEM_OUT: {
                handler.setTarget(ConsoleHandler.Target.SYSTEM_OUT);
                break;
            }
        }
    }

    public synchronized void stop(final StopContext context) {
        final ConsoleHandler handler = value;
        handler.close();
        value = null;
    }

    public synchronized Handler getValue() throws IllegalStateException {
        return value;
    }

    public synchronized Level getLevel() {
        return level;
    }

    public synchronized void setLevel(final Level level) {
        this.level = level;
        final ConsoleHandler handler = value;
        if (handler != null) handler.setLevel(level);
    }

    public synchronized AbstractFormatterSpec getFormatterSpec() {
        return formatterSpec;
    }

    public synchronized void setFormatterSpec(final AbstractFormatterSpec formatterSpec) {
        this.formatterSpec = formatterSpec;
        final ConsoleHandler handler = value;
        if (handler != null) formatterSpec.apply(handler);
    }

    public synchronized Target getTarget() {
        return target;
    }

    public synchronized void setTarget(final Target target) {
        this.target = target;
        setTarget(value, target);
    }

    public synchronized boolean isAutoflush() {
        return autoflush;
    }

    public synchronized void setAutoflush(final boolean autoflush) {
        this.autoflush = autoflush;
        final ConsoleHandler handler = value;
        if (handler != null) handler.setAutoFlush(autoflush);
    }

    public synchronized String getEncoding() {
        return encoding;
    }

    public synchronized void setEncoding(final String encoding) throws UnsupportedEncodingException {
        final ConsoleHandler handler = value;
        if (handler != null) handler.setEncoding(encoding);
        this.encoding = encoding;
    }
}
