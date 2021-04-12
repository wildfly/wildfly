/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow;

import io.undertow.attribute.ExchangeAttribute;
import io.undertow.predicate.Predicate;
import io.undertow.predicate.Predicates;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.accesslog.AccessLogHandler;
import io.undertow.server.handlers.accesslog.AccessLogReceiver;
import io.undertow.server.handlers.accesslog.DefaultAccessLogReceiver;
import io.undertow.server.handlers.accesslog.ExtendedAccessLogParser;
import io.undertow.server.handlers.accesslog.JBossLoggingAccessLogReceiver;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.undertow.logging.UndertowLogger;
import org.xnio.IoUtils;
import org.xnio.XnioWorker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class AccessLogService implements Service<AccessLogService> {
    private final Consumer<AccessLogService> serviceConsumer;
    private final Supplier<Host> host;
    private final Supplier<XnioWorker> worker;
    private final Supplier<PathManager> pathManager;
    private final String pattern;
    private final String path;
    private final String pathRelativeTo;
    private final String filePrefix;
    private final String fileSuffix;
    private final boolean rotate;
    private final boolean useServerLog;
    private final boolean extended;
    private final Predicate predicate;
    private volatile AccessLogReceiver logReceiver;

    private PathManager.Callback.Handle callbackHandle;
    private Path directory;
    private ExchangeAttribute extendedPattern;

    AccessLogService(final Consumer<AccessLogService> serviceConsumer, final Supplier<Host> host,
                     final Supplier<XnioWorker> worker, final Supplier<PathManager> pathManager,
                     final String pattern, final boolean extended, final Predicate predicate) {
        this(serviceConsumer, host, worker, pathManager, pattern, null, null, null, null, false, extended, predicate);
    }

    AccessLogService(final Consumer<AccessLogService> serviceConsumer, final Supplier<Host> host,
                     final Supplier<XnioWorker> worker, final Supplier<PathManager> pathManager,
                     final String pattern, final String path, final String pathRelativeTo,
                     final String filePrefix, final String fileSuffix, final boolean rotate,
                     final boolean extended, final Predicate predicate) {
        this.serviceConsumer = serviceConsumer;
        this.host = host;
        this.worker = worker;
        this.pathManager = pathManager;
        this.pattern = pattern;
        this.path = path;
        this.pathRelativeTo = pathRelativeTo;
        this.filePrefix = filePrefix;
        this.fileSuffix = fileSuffix;
        this.rotate = rotate;
        this.extended = extended;
        this.useServerLog = false;
        this.predicate = predicate == null ? Predicates.truePredicate() : predicate;
    }

    @Override
    public void start(StartContext context) throws StartException {
        if (useServerLog) {
            logReceiver = new JBossLoggingAccessLogReceiver();
        } else {
            if (pathRelativeTo != null) {
                callbackHandle = pathManager.get().registerCallback(pathRelativeTo, PathManager.ReloadServerCallback.create(), PathManager.Event.UPDATED, PathManager.Event.REMOVED);
            }
            directory = Paths.get(pathManager.get().resolveRelativePathEntry(path, pathRelativeTo));
            if (!Files.exists(directory)) {
                try {
                    Files.createDirectories(directory);
                } catch (IOException e) {
                    throw UndertowLogger.ROOT_LOGGER.couldNotCreateLogDirectory(directory, e);
                }
            }
            try {
                DefaultAccessLogReceiver.Builder builder = DefaultAccessLogReceiver.builder().setLogWriteExecutor(worker.get())
                        .setOutputDirectory(directory)
                        .setLogBaseName(filePrefix)
                        .setLogNameSuffix(fileSuffix)
                        .setRotate(rotate);
                if(extended) {
                    builder.setLogFileHeaderGenerator(new ExtendedAccessLogParser.ExtendedAccessLogHeaderGenerator(pattern));
                    extendedPattern = new ExtendedAccessLogParser(getClass().getClassLoader()).parse(pattern);
                } else {
                    extendedPattern = null;
                }
                logReceiver = builder.build();
            } catch (IllegalStateException e) {
                throw new StartException(e);
            }
        }
        host.get().setAccessLogService(this);
        serviceConsumer.accept(this);
    }

    @Override
    public void stop(StopContext context) {
        serviceConsumer.accept(null);
        host.get().setAccessLogService(null);
        if (callbackHandle != null) {
            callbackHandle.remove();
            callbackHandle = null;
        }
        if( logReceiver instanceof DefaultAccessLogReceiver ) {
            IoUtils.safeClose((DefaultAccessLogReceiver) logReceiver);
        }
        logReceiver = null;
    }

    protected AccessLogHandler configureAccessLogHandler(HttpHandler handler) {
        if(extendedPattern != null) {
            return new AccessLogHandler(handler, logReceiver, pattern, extendedPattern, predicate);
        } else {
            return new AccessLogHandler(handler, logReceiver, pattern, getClass().getClassLoader(), predicate);
        }
    }

    boolean isRotate() {
        return rotate;
    }

    String getPath() {
        return path;
    }

    @Override
    public AccessLogService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }
}
