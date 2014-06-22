/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.wildfly.extension.undertow;

import java.io.File;

import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.accesslog.AccessLogHandler;
import io.undertow.server.handlers.accesslog.AccessLogReceiver;
import io.undertow.server.handlers.accesslog.DefaultAccessLogReceiver;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.undertow.logging.UndertowLogger;
import org.xnio.XnioWorker;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 */
class AccessLogService implements Service<AccessLogService> {
    protected final InjectedValue<XnioWorker> worker = new InjectedValue<>();
    private final String pattern;
    private final File directory;
    private final String filePrefix;
    private final String fileSuffix;
    private volatile AccessLogReceiver logReceiver;

    AccessLogService(String pattern, File directory, String filePrefix, String fileSuffix) {
        this.pattern = pattern;
        this.directory = directory;
        this.filePrefix = filePrefix;
        this.fileSuffix = fileSuffix;
    }

    @Override
    public void start(StartContext context) throws StartException {
        if (!directory.exists()) {
            if (!directory.mkdirs()){
                throw UndertowLogger.ROOT_LOGGER.couldNotCreateLogDirectory(directory);
            }
        }
        try {
            logReceiver = new DefaultAccessLogReceiver(worker.getValue(), directory, filePrefix, fileSuffix);
        } catch (IllegalStateException e) {
            throw new StartException(e);
        }
    }

    @Override
    public void stop(StopContext context) {

    }

    @Override
    public AccessLogService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    InjectedValue<XnioWorker> getWorker() {
        return worker;
    }

    protected AccessLogHandler configureAccessLogHandler(HttpHandler handler) {
        return new AccessLogHandler(handler, logReceiver, pattern, AccessLogHandler.class.getClassLoader());
    }

}
