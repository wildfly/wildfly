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

package org.jboss.as.logging.handlers.file;

import java.io.FileNotFoundException;

import org.jboss.as.logging.handlers.FlushingHandlerService;
import org.jboss.logmanager.handlers.FileHandler;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Values;

/**
 * Date: 23.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
abstract class AbstractFileHandlerService<T extends FileHandler> extends FlushingHandlerService<T> {

    private final InjectedValue<String> fileName = new InjectedValue<String>();
    private boolean append;

    @Override
    protected void start(final StartContext context, final T handler) throws StartException {
        handler.setAutoFlush(isAutoflush());
        handler.setAppend(append);
        try {
            handler.setFileName(getFileName());
        } catch (FileNotFoundException e) {
            throw new StartException(e);
        }
    }

    public final synchronized boolean isAppend() {
        return append;
    }

    public final synchronized void setAppend(final boolean append) {
        this.append = append;
        final T handler = getValue();
        if (handler != null) {
            handler.setAppend(append);
        }
    }

    public final synchronized void setFile(final String path) throws FileNotFoundException {
        fileName.setValue(Values.immediateValue(path));
        final T handler = getValue();
        if (handler != null) {
            handler.setFileName(path);
        }
    }

    public final synchronized String getFileName() {
        return fileName.getValue();
    }

    public final synchronized Injector<String> getFileNameInjector() {
        return fileName;
    }
}
