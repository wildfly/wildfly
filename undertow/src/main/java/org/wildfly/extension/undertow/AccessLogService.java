/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
    private final int closeRetryCount;
    private final int closeRetryDelay;
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
        this(serviceConsumer, host, worker, pathManager, pattern, null, null, null, null, false, extended, true, predicate, -1 , -1);
    }

    AccessLogService(final Consumer<AccessLogService> serviceConsumer, final Supplier<Host> host,
                     final Supplier<XnioWorker> worker, final Supplier<PathManager> pathManager,
                     final String pattern, final String path, final String pathRelativeTo,
                     final String filePrefix, final String fileSuffix, final boolean rotate,
                     final boolean extended, final boolean useServerLog, final Predicate predicate, final int closeRetryCount, final int closeRetryDelay) {
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
        this.useServerLog = useServerLog;
        this.predicate = predicate == null ? Predicates.truePredicate() : predicate;
        this.closeRetryCount = closeRetryCount;
        this.closeRetryDelay = closeRetryDelay;
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
                        .setRotate(rotate)
                        .setCloseRetryDelay(closeRetryDelay)
                        .setCloseRetryCount(closeRetryCount);
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
        if (logReceiver instanceof DefaultAccessLogReceiver) {
            final Thread loggerStreamCloser = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        IoUtils.safeClose((DefaultAccessLogReceiver) logReceiver);
                    } finally {
                        logReceiver = null;
                        context.complete();
                    }

                }
            }, "Access Log Shutdown Thread");
            context.asynchronous();
            loggerStreamCloser.start();
        } else {
            logReceiver = null;
        }
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
