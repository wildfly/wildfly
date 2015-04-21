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

import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.accesslog.AccessLogHandler;
import io.undertow.server.handlers.accesslog.AccessLogReceiver;
import io.undertow.server.handlers.accesslog.DefaultAccessLogReceiver;
import io.undertow.server.handlers.accesslog.JBossLoggingAccessLogReceiver;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.undertow.logging.UndertowLogger;
import org.xnio.XnioWorker;

import java.io.File;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 */
class AccessLogService implements Service<AccessLogService> {
    protected final InjectedValue<XnioWorker> worker = new InjectedValue<>();
    private final String pattern;
    private final String path;
    private final String pathRelativeTo;
    private final String filePrefix;
    private final String fileSuffix;
    private final boolean useServerLog;
    private volatile AccessLogReceiver logReceiver;


    private PathManager.Callback.Handle callbackHandle;

    private File directory;

    private final InjectedValue<PathManager> pathManager = new InjectedValue<PathManager>();


    AccessLogService(String pattern) {
        this.pattern = pattern;
        this.path = null;
        this.pathRelativeTo = null;
        this.filePrefix = null;
        this.fileSuffix = null;
        this.useServerLog = true;
    }

    AccessLogService(String pattern, String path, String pathRelativeTo, String filePrefix, String fileSuffix) {
        this.pattern = pattern;
        this.path = path;
        this.pathRelativeTo = pathRelativeTo;
        this.filePrefix = filePrefix;
        this.fileSuffix = fileSuffix;
        this.useServerLog = false;
    }

    @Override
    public void start(StartContext context) throws StartException {
        if (useServerLog) {
            logReceiver = new JBossLoggingAccessLogReceiver();
        } else {
            if (pathRelativeTo != null) {
                callbackHandle = pathManager.getValue().registerCallback(pathRelativeTo, PathManager.ReloadServerCallback.create(), PathManager.Event.UPDATED, PathManager.Event.REMOVED);
            }
            directory = new File(pathManager.getValue().resolveRelativePathEntry(path, pathRelativeTo));
            if (!directory.exists()) {
                if (!directory.mkdirs()) {
                    throw UndertowLogger.ROOT_LOGGER.couldNotCreateLogDirectory(directory);
                }
            }
            try {
                logReceiver = new DefaultAccessLogReceiver(worker.getValue(), directory, filePrefix, fileSuffix);
            } catch (IllegalStateException e) {
                throw new StartException(e);
            }
        }
    }

    @Override
    public void stop(StopContext context) {
        if (callbackHandle != null) {
            callbackHandle.remove();
            callbackHandle = null;
        }
    }

    @Override
    public AccessLogService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    InjectedValue<XnioWorker> getWorker() {
        return worker;
    }

    InjectedValue<PathManager> getPathManager() {
        return pathManager;
    }

    protected AccessLogHandler configureAccessLogHandler(HttpHandler handler) {
        return new AccessLogHandler(handler, logReceiver, pattern, AccessLogService.class.getClassLoader());
    }

}
