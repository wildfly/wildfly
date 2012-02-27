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

import org.jboss.as.controller.services.path.AbstractPathService;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Service responsible for determining the correct log file path for a logging handler.
 *
 * @author John Bailey
 */
public class HandlerFileService implements Service<String> {
    private static final char SEPARATOR = '/';
    private final InjectedValue<String> relativeTo = new InjectedValue<String>();
    private String path;

    private volatile String fileName;

    public HandlerFileService(final String path) {
        this.path = path;
    }

    public synchronized void start(StartContext context) throws StartException {
        final String value = relativeTo.getOptionalValue();
        fileName = value != null && !AbstractPathService.isAbsoluteUnixOrWindowsPath(path) ? (value + SEPARATOR + path) : path;
    }

    public synchronized void stop(StopContext context) {
        fileName = null;
    }

    public synchronized void setPath(final String path) {
        this.path = path;
        final String value = relativeTo.getOptionalValue();
        fileName = value != null && !AbstractPathService.isAbsoluteUnixOrWindowsPath(path) ? (value + SEPARATOR + path) : path;
    }

    public String getValue() throws IllegalStateException, IllegalArgumentException {
        return fileName;
    }

    public Injector<String> getRelativeToInjector() {
        return relativeTo;
    }
}
