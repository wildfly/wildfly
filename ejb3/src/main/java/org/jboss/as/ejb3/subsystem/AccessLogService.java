/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.subsystem;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.ejb3.component.AccessLogHolder;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.event.logger.Event;
import org.wildfly.event.logger.EventLogger;
import org.wildfly.event.logger.EventWriter;
import org.wildfly.event.logger.FileEventWriter;
import org.wildfly.event.logger.JsonEventFormatter;
import org.wildfly.event.logger.LoggerEventWriter;
import org.wildfly.event.logger.StdoutEventWriter;
import org.xnio.XnioWorker;

/**
 * MSC service that owns the {@link EventLogger} for EJB access logging.
 *
 * <p>On {@link #start}: selects the {@link EventWriter} from the {@code destination}
 * attribute ({@code console} → {@link StdoutEventWriter}, {@code logging} →
 * {@link LoggerEventWriter}, {@code file} → {@link FileEventWriter}), wraps it in an
 * async {@link EventLogger} backed by the XNIO {@code worker}, and publishes the logger
 * for the interceptor (E3) to read.
 *
 * <p>On {@link #stop}: nulls the published logger, drains whatever the async logger still
 * holds, and closes the writer cleanly — in that order, so that queued events are written
 * before the writer underneath them goes away.
 *
 * <p>{@link #getEventsLogged()} and {@link #getEventsDropped()} expose the runtime
 * metrics registered in {@link AccessLogResourceDefinition}.
 */
public class AccessLogService implements Service {

    static final String LOG_CATEGORY = "org.jboss.as.ejb3.access-log";
    static final String EVENT_SOURCE = "ejb-access";

    /** All 21 tokens enabled — used when the model attribute is UNDEFINED. */
    static final Set<AccessLogResourceDefinition.AttributeVocabulary> ALL_ATTRIBUTES =
            EnumSet.allOf(AccessLogResourceDefinition.AttributeVocabulary.class);

    private final Consumer<AccessLogService> serviceConsumer;
    private final Supplier<AccessLogHolder> holderSupplier;
    private final Supplier<XnioWorker> worker;
    private final Supplier<PathManager> pathManager;

    // Configuration — immutable (RESTART_RESOURCE_SERVICES attributes)
    private final String destination;
    /** Relative file path component (e.g. "ejb-access.log"). May be null only when destination != file. */
    private final String path;
    /** Base path name passed to PathManager (e.g. "jboss.server.log.dir"). Null means treat path as absolute. */
    private final String relativeTo;
    private final String rotateSuffix;
    /**
     * True when the operator explicitly supplied at least one of path/relative-to/rotate-suffix.
     * Used to detect the expression-destination case at service start.
     */
    private final boolean fileAttrsExplicitlySet;
    /** Enabled log fields; defaults to all tokens when the model attribute is UNDEFINED. */
    private final Set<AccessLogResourceDefinition.AttributeVocabulary> enabledAttributes;
    /** Maximum async queue depth. Corresponds to the {@code queue-length} management attribute. */
    private final int queueLength;

    // Live-mutable configuration (RESTART_NONE attributes)
    private volatile boolean includeLocal;
    private volatile boolean includeNodeName;
    private volatile java.util.Map<String, Object> metadata;

    // Mutable state published for the interceptor
    private volatile EventLogger eventLogger;

    // Metrics
    // Counter values live on AccessLogHolder so they survive RESTART_RESOURCE_SERVICES lifecycle.

    /**
     * Guards the log-once behaviour for emit failures: set to {@code true} after the first
     * failure has been logged at ERROR; subsequent failures are demoted to DEBUG.
     * Reset to {@code false} on service start so that a restart surfaces a fresh ERROR.
     */
    private final AtomicBoolean emitFailureLogged = new AtomicBoolean();

    // The writer to close on stop
    private EventWriter activeWriter;

    AccessLogService(
            final Consumer<AccessLogService> serviceConsumer,
            final Supplier<AccessLogHolder> holderSupplier,
            final Supplier<XnioWorker> worker,
            final Supplier<PathManager> pathManager,
            final String destination,
            final String path,
            final String relativeTo,
            final String rotateSuffix,
            final boolean fileAttrsExplicitlySet,
            final Set<AccessLogResourceDefinition.AttributeVocabulary> enabledAttributes,
            final boolean includeLocal,
            final boolean includeNodeName,
            final java.util.Map<String, Object> metadata,
            final int queueLength) {
        this.serviceConsumer = serviceConsumer;
        this.holderSupplier = holderSupplier;
        this.worker = worker;
        this.pathManager = pathManager;
        this.destination = destination;
        this.path = path;
        this.relativeTo = relativeTo;
        this.rotateSuffix = rotateSuffix;
        this.fileAttrsExplicitlySet = fileAttrsExplicitlySet;
        this.enabledAttributes = enabledAttributes;
        this.includeLocal = includeLocal;
        this.includeNodeName = includeNodeName;
        this.metadata = metadata;
        this.queueLength = queueLength;
    }

    @Override
    public void start(final StartContext context) throws StartException {
        final JsonEventFormatter.Builder formatterBuilder = JsonEventFormatter.builder();
        if (metadata != null && !metadata.isEmpty()) {
            formatterBuilder.addMetaData(metadata);
        }
        final JsonEventFormatter formatter = formatterBuilder.build();

        final EventWriter writer;
        try {
            writer = buildWriter(formatter);
        } catch (IOException e) {
            throw new StartException("Failed to open access-log writer for destination '" + destination + "'", e);
        }
        this.activeWriter = writer;

        // Wrap with a counting writer so we can report events-logged.
        final AccessLogHolder holder = holderSupplier.get();
        final EventWriter countingWriter = new CountingEventWriter(writer, holder);

        this.eventLogger = EventLogger.createAsyncLogger(EVENT_SOURCE, countingWriter, worker.get(), queueLength);
        emitFailureLogged.set(false);
        holderSupplier.get().set(this);
        serviceConsumer.accept(this);
    }

    @Override
    public void stop(final StopContext context) {
        final AccessLogHolder holder = holderSupplier.get();
        if (holder != null) {
            holder.clear();
        }
        serviceConsumer.accept(null);
        final EventLogger el = this.eventLogger;
        this.eventLogger = null;
        if (el != null) {
            try {
                // Drains the async queue on this thread. Must run before the writer is
                // closed, otherwise the events it drains have nowhere to go.
                el.close();
            } catch (Exception ignored) {
                // best effort
            }
            // Read the drop count only after close(), so that events rejected during the
            // drain are included, and fold it into the holder's counter: once the logger
            // reference is gone getEventsDropped() can no longer reach it, and the metric
            // must not fall back.
            if (holder != null) {
                holder.addEventsDropped(el.getDroppedCount());
            }
        }
        final EventWriter w = this.activeWriter;
        this.activeWriter = null;
        if (w != null) {
            try {
                w.close();
            } catch (Exception ignored) {
                // best effort
            }
        }
    }

    /**
     * Returns the live {@link EventLogger}, or {@code null} if the service is not running.
     * The interceptor (E3) reads this field on every invocation.
     */
    public EventLogger getEventLogger() {
        return eventLogger;
    }

    /**
     * Installs the live {@link EventLogger} without going through {@link #start}.
     *
     * <p>Package-private seam for {@code AccessLogServiceTest}: constructing the real
     * logger in {@link #start} needs an {@link XnioWorker}, which a unit test has no way
     * to supply usefully. Production code never calls this.
     */
    void setEventLogger(final EventLogger eventLogger) {
        this.eventLogger = eventLogger;
    }

    /**
     * Returns {@code true} if the log-once guard has fired at least once — i.e. if the
     * first emit failure has already been logged at ERROR.
     *
     * <p>Package-private seam for {@code AccessLogServiceTest} to verify the CAS branch
     * in {@link #recordEmitFailure} without capturing log records. Production code never
     * calls this.
     */
    boolean isEmitFailureLoggedOnce() {
        return emitFailureLogged.get();
    }

    /** Returns the set of enabled log field tokens. Never null. */
    public Set<AccessLogResourceDefinition.AttributeVocabulary> getEnabledAttributes() {
        return enabledAttributes;
    }

    /** Returns true if local (in-VM) invocations should be logged. */
    public boolean isIncludeLocal() {
        return includeLocal;
    }

    /** Returns true if the node name should be included in every record. */
    public boolean isIncludeNodeName() {
        return includeNodeName;
    }

    /** Sets include-local flag (live-mutable, RESTART_NONE). */
    public void setIncludeLocal(final boolean includeLocal) {
        this.includeLocal = includeLocal;
    }

    /** Sets include-node-name flag (live-mutable, RESTART_NONE). */
    public void setIncludeNodeName(final boolean includeNodeName) {
        this.includeNodeName = includeNodeName;
    }

    /** Sets metadata map (live-mutable, RESTART_NONE). */
    public void setMetadata(final java.util.Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    /** Running total of events handed to the writer. */
    public long getEventsLogged() {
        final AccessLogHolder holder = holderSupplier.get();
        return holder != null ? holder.getEventsLogged() : 0L;
    }

    /**
     * Running total of dropped events, reported as the sum of two independent sources:
     * <ol>
     *   <li><em>Emit failures</em> — invocations where the {@code emit()} call in
     *       {@link org.jboss.as.ejb3.component.EjbAccessLogInterceptor} threw an unexpected
     *       exception. Each such failure increments this counter by one.</li>
     *   <li><em>Queue overflow</em> — events that the underlying {@link EventLogger}
     *       could not enqueue because the async queue was full or already closed, as
     *       reported by {@link EventLogger#getDroppedCount()}.</li>
     * </ol>
     * Both sources represent the same administrative fact: the access log is incomplete
     * and an audit trail may be lossy.
     *
     * <p>The value is monotonic across a {@link #stop}: the logger's own count is folded
     * into the holder's counter before the reference is dropped.
     */
    public long getEventsDropped() {
        final AccessLogHolder holder = holderSupplier.get();
        final long base = holder != null ? holder.getEventsDropped() : 0L;
        final EventLogger el = eventLogger;
        return base + (el != null ? el.getDroppedCount() : 0L);
    }

    /**
     * Records one emit failure: increments the dropped-events counter and handles
     * the log-once behaviour.  The first failure is logged at ERROR (with the full
     * stack trace); every subsequent failure is logged at DEBUG so that a persistently
     * broken emit does not flood the server log.
     *
     * <p>The logging calls are themselves guarded against further exceptions so that
     * an error in the logging path cannot re-introduce the problem one level up.
     *
     * @param cause the throwable thrown by the emit call
     */
    public void recordEmitFailure(final Throwable cause) {
        final AccessLogHolder holder = holderSupplier.get();
        if (holder != null) {
            holder.incrementEventsDropped();
        }
        try {
            if (emitFailureLogged.compareAndSet(false, true)) {
                EjbLogger.ROOT_LOGGER.accessLogEmitFailed(cause);
            } else {
                EjbLogger.ROOT_LOGGER.debug("EJB access-log emit failed (suppressed; see earlier ERROR for stack trace)", cause);
            }
        } catch (final Throwable ignored) {
            // Guard: never let the logging path itself propagate.
        }
    }

    // -------------------------------------------------------------------------

    private EventWriter buildWriter(final JsonEventFormatter formatter) throws IOException, StartException {
        switch (destination) {
            case "console":
                warnFileAttributesIfSet(destination);
                return StdoutEventWriter.of(formatter);
            case "logging":
                warnFileAttributesIfSet(destination);
                return LoggerEventWriter.of(LOG_CATEGORY, formatter);
            case "file":
            default: {
                final String resolved = pathManager.get().resolveRelativePathEntry(path, relativeTo);
                final Path filePath = Paths.get(resolved);
                if (filePath.getParent() != null) {
                    java.nio.file.Files.createDirectories(filePath.getParent());
                }
                return FileEventWriter.open(filePath, formatter, rotateSuffix);
            }
        }
    }

    /**
     * Fails the service start if file-only attributes were explicitly supplied alongside a
     * non-file destination. This catches the expression-destination case that model-time
     * validation cannot see because the destination value was an expression.
     */
    private void warnFileAttributesIfSet(final String resolvedDestination) throws StartException {
        if (fileAttrsExplicitlySet) {
            throw EjbLogger.ROOT_LOGGER.fileAttributesIgnoredForNonFileDestination(resolvedDestination);
        }
    }

    // -------------------------------------------------------------------------

    /**
     * Thin wrapper that increments a counter on each successful write.
     */
    private static final class CountingEventWriter implements EventWriter {
        private final EventWriter delegate;
        private final AccessLogHolder holder;

        CountingEventWriter(final EventWriter delegate, final AccessLogHolder holder) {
            this.delegate = delegate;
            this.holder = holder;
        }

        @Override
        public void write(final Event event) {
            delegate.write(event);
            if (holder != null) {
                holder.incrementEventsLogged();
            }
        }

        @Override
        public void close() throws Exception {
            delegate.close();
        }
    }
}
