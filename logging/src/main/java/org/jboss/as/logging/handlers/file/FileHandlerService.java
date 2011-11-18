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

package org.jboss.as.logging.handlers.file;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.jboss.as.logging.handlers.FormatterSpec;
import org.jboss.as.logging.handlers.FlushingHandlerService;
import org.jboss.logmanager.handlers.FileHandler;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class FileHandlerService implements FlushingHandlerService {

    private final InjectedValue<String> fileName = new InjectedValue<String>();

    private Filter filter;
    private FormatterSpec formatterSpec;
    private Level level;
    private boolean autoflush;
    private String encoding;
    private boolean append;
    private FileHandler value;

    public synchronized void start(final StartContext context) throws StartException {
        final FileHandler handler = new FileHandler();
        value = handler;
        if (filter != null) handler.setFilter(filter);
        formatterSpec.apply(handler);
        if (level != null) handler.setLevel(level);
        handler.setAutoFlush(autoflush);
        try {
            handler.setEncoding(encoding);
        } catch (UnsupportedEncodingException e) {
            throw new StartException(e);
        }
        handler.setAppend(append);
        try {
            handler.setFileName(fileName.getValue());
        } catch (FileNotFoundException e) {
            throw new StartException(e);
        }
        value = handler;
    }

    public synchronized void stop(final StopContext context) {
        final FileHandler handler = value;
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
        final FileHandler handler = value;
        if (handler != null) handler.setLevel(level);
    }

    public synchronized FormatterSpec getFormatterSpec() {
        return formatterSpec;
    }

    public synchronized void setFormatterSpec(final FormatterSpec formatterSpec) {
        this.formatterSpec = formatterSpec;
        final FileHandler handler = value;
        if (handler != null) formatterSpec.apply(handler);
    }

    @Override
    public synchronized void setFilter(final Filter filter) {
        this.filter = filter;
        final FileHandler handler = value;
        if (handler != null) {
            handler.setFilter(filter);
        }
    }

    public synchronized boolean isAutoflush() {
        return autoflush;
    }

    public synchronized void setAutoflush(final boolean autoflush) {
        this.autoflush = autoflush;
        final FileHandler handler = value;
        if (handler != null) handler.setAutoFlush(autoflush);
    }

    public synchronized String getEncoding() {
        return encoding;
    }

    public synchronized void setEncoding(final String encoding) throws UnsupportedEncodingException {
        final FileHandler handler = value;
        if (handler != null) handler.setEncoding(encoding);
        this.encoding = encoding;
    }

    public synchronized boolean isAppend() {
        return append;
    }

    public synchronized void setAppend(final boolean append) {
        this.append = append;
        final FileHandler handler = value;
        if (handler != null) handler.setAppend(append);
    }

    public Injector<String> getFileNameInjector() {
        return fileName;
    }
}
