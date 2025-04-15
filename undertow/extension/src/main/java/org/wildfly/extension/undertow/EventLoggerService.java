/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

import io.undertow.predicate.Predicate;
import io.undertow.predicate.Predicates;
import io.undertow.server.HttpHandler;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.event.logger.EventLogger;
import org.wildfly.event.logger.JsonEventFormatter;
import org.wildfly.event.logger.StdoutEventWriter;
import org.wildfly.extension.undertow.logging.UndertowLogger;
import org.xnio.XnioWorker;

/**
 * A service which creates an asynchronous {@linkplain EventLogger event logger} which writes to {@code stdout} in JSON
 * structured format.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class EventLoggerService implements Service {
    private final Set<AccessLogAttribute> attributes;
    private final boolean includeHostName;
    private final Map<String, Object> metadata;
    private final Predicate predicate;
    private final Supplier<Host> host;
    private final Supplier<XnioWorker> worker;

    /**
     * Creates a new service.
     *
     * @param predicate       the predicate that determines if the request should be logged
     * @param metadata        a map of metadata to be prepended to the structured output
     * @param includeHostName {@code true} to include the host name in the structured JSON output
     * @param host            the host service supplier
     * @param worker          the worker service supplier for the
     *                        {@linkplain EventLogger#createAsyncLogger(String, Executor) async logger}
     */
    EventLoggerService(final Collection<AccessLogAttribute> attributes, final Predicate predicate, final Map<String, Object> metadata,
                       final boolean includeHostName, final Supplier<Host> host, final Supplier<XnioWorker> worker) {
        this.attributes = new CopyOnWriteArraySet<>(attributes);
        this.predicate = predicate == null ? Predicates.truePredicate() : predicate;
        this.metadata = metadata;
        this.includeHostName = includeHostName;
        this.host = host;
        this.worker = worker;
    }

    @Override
    @SuppressWarnings("Convert2Lambda")
    public void start(final StartContext context) throws StartException {
        final Host host = this.host.get();
        // Create the JSON event formatter
        final JsonEventFormatter.Builder formatterBuilder = JsonEventFormatter.builder()
                .setIncludeTimestamp(false);
        if (includeHostName) {
            formatterBuilder.addMetaData("hostName", host.getName());
        }
        if (metadata != null && !metadata.isEmpty()) {
            formatterBuilder.addMetaData(metadata);
        }
        final JsonEventFormatter formatter = formatterBuilder.build();
        final EventLogger eventLogger = EventLogger.createAsyncLogger("web-access",
                StdoutEventWriter.of(formatter), worker.get());
        UndertowLogger.ROOT_LOGGER.debugf("Adding console-access-log for host %s", host.getName());
        host.setAccessLogHandler(new Function<HttpHandler, HttpHandler>() {
            @Override
            public HttpHandler apply(final HttpHandler httpHandler) {
                return new EventLoggerHttpHandler(httpHandler, predicate, attributes, eventLogger);
            }
        });
    }

    @Override
    public void stop(final StopContext context) {
        final Host host = this.host.get();
        UndertowLogger.ROOT_LOGGER.debugf("Removing console-access-log for host %s", host.getName());
        host.setAccessLogHandler(null);
    }
}
