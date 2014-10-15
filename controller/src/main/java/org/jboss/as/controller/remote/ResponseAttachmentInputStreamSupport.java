/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.remote;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTACHED_STREAMS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESPONSE_HEADERS;
import static org.jboss.as.protocol.mgmt.ProtocolUtils.expectHeader;

import java.io.Closeable;
import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.as.controller.ControllerLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.client.OperationResponse;
import org.jboss.as.controller.client.impl.ModelControllerProtocol;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.protocol.mgmt.ActiveOperation;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementProtocol;
import org.jboss.as.protocol.mgmt.ManagementRequestContext;
import org.jboss.as.protocol.mgmt.ManagementRequestHandler;
import org.jboss.as.protocol.mgmt.ManagementRequestHeader;
import org.jboss.as.protocol.mgmt.ManagementResponseHeader;
import org.jboss.dmr.ModelNode;

/**
 * Support logic related to dealing with input streams attached to an operation response.
 *
 * @author Brian Stansberry (c) 2014 Red Hat Inc.
 */
public class ResponseAttachmentInputStreamSupport {

    /** Timeout for cleaning up streams that have not been read by the end user */
    private static final int STREAM_TIMEOUT = 30000;
    /** Timeout for cleaning up streams that have not been read by the end user */
    private static final int CLEANUP_INTERVAL = 10000;

    /**
     * Deal with streams attached to an operation response from a proxied domain process.
     *
     * @param context the context of the operation
     * @param responseNode the DMR response from the proxied process
     * @param streams the streams associated with the response
     */
    public static void handleDomainOperationResponseStreams(final OperationContext context,
                                                            final ModelNode responseNode,
                                                            final List<OperationResponse.StreamEntry> streams) {

        if (responseNode.hasDefined(RESPONSE_HEADERS)) {
            ModelNode responseHeaders = responseNode.get(RESPONSE_HEADERS);
            // Strip out any stream header as the header created by this process is what counts
            responseHeaders.remove(ATTACHED_STREAMS);
            if (responseHeaders.asInt() == 0) {
                responseNode.remove(RESPONSE_HEADERS);
            }
        }

        for (OperationResponse.StreamEntry streamEntry : streams) {
            context.attachResultStream(streamEntry.getUUID(), streamEntry.getMimeType(), streamEntry.getStream());
        }
    }

    private final Map<InputStreamKey, TimedStreamEntry> streamMap  = new ConcurrentHashMap<InputStreamKey, TimedStreamEntry>();
    private final ScheduledFuture<?> cleanupTaskFuture;
    private final int timeout;
    private volatile boolean stopped;

    /**
     * <strong>For test usage only</strong> as it has no facility for closing attached streams.
     */
    public ResponseAttachmentInputStreamSupport() {
        this(STREAM_TIMEOUT);
    }

    /** Package protected constructor to allow unit tests to control timing of cleanup. */
    ResponseAttachmentInputStreamSupport(int streamTimeout) {
        this.timeout = streamTimeout;
        cleanupTaskFuture = null;
    }

    /**
     * Create a new support with the given timeout for closing unread streams.
     *
     * @param scheduledExecutorService scheduled executor to use to periodically clean up unused streams. Cannot be {@code null}
     */
    public ResponseAttachmentInputStreamSupport(ScheduledExecutorService scheduledExecutorService) {
        this(scheduledExecutorService, STREAM_TIMEOUT, CLEANUP_INTERVAL);
    }

    /** Package protected constructor to allow unit tests to control timing of cleanup. */
    ResponseAttachmentInputStreamSupport(ScheduledExecutorService scheduledExecutorService, int streamTimeout, int cleanupInterval) {
        timeout = streamTimeout;
        cleanupTaskFuture = scheduledExecutorService.scheduleWithFixedDelay(new CleanupTask(), cleanupInterval, cleanupInterval, TimeUnit.MILLISECONDS);
    }

    /**
     * Registers a set of streams that were associated with a particular request. Does nothing if {@link #shutdown()}
     * has been invoked, in which case any use of the {@link #getReadHandler() read handler} will result in behavior
     * equivalent to what would be seen if the the registered stream had 0 bytes of content.
     *
     * @param operationId id of the request
     * @param streams the streams. Cannot be {@code null} but may be empty
     */
    synchronized void registerStreams(int operationId, List<OperationResponse.StreamEntry> streams) {
        // ^^^ synchronize on 'this' to avoid races with shutdown

        if (!stopped) {
            // Streams share a timestamp so activity on any is sufficient to keep the rest alive
            AtomicLong timestamp = new AtomicLong(System.currentTimeMillis());
            for (int i = 0; i < streams.size(); i++) {
                OperationResponse.StreamEntry stream = streams.get(i);
                InputStreamKey key = new InputStreamKey(operationId, i);
                streamMap.put(key, new TimedStreamEntry(stream, timestamp));
            }
        } else {
            // Just close the streams, as no caller ever will
            for (int i = 0; i < streams.size(); i++) {
                closeStreamEntry(streams.get(i), operationId, i);
            }
        }
    }

    /**
     * Gets a handler for requests to read an input stream.
     *
     * @return  the handler
     */
    ManagementRequestHandler<Void, Void> getReadHandler() {
        return new ReadHandler();
    }

    /**
     * Gets a handler for requests to close an input stream.
     *
     * @return  the handler
     */
    ManagementRequestHandler<Void, Void> getCloseHandler() {
        return new AbstractAttachmentHandler() {
            @Override
            void handleRequest(TimedStreamEntry entry, FlushableDataOutput output) throws IOException {
                // no-op as AbstractAttachmentHandler will close the entry after calling this
            }

            @Override
            void handleMissingStream(int requestId, int index, FlushableDataOutput output) throws IOException {
                // no-op as there's nothing to do
            }
        };
    }

    /**
     * Closes any registered stream entries that have not yet been consumed
     */
    public final synchronized void shutdown() {  // synchronize on 'this' to avoid races with registerStreams
        stopped = true;
        // If the cleanup task is running tell it to stop looping, and then remove it from the scheduled executor
        if (cleanupTaskFuture != null) {
            cleanupTaskFuture.cancel(false);
        }

        // Close remaining streams
        for (Map.Entry<InputStreamKey, TimedStreamEntry> entry : streamMap.entrySet()) {
            InputStreamKey key = entry.getKey();
            TimedStreamEntry timedStreamEntry = entry.getValue();
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (timedStreamEntry) { // ensure there's no race with a request that got a ref before we removed it
                closeStreamEntry(timedStreamEntry, key.requestId, key.index);
            }
        }
    }

    /** Close and remove expired streams. Package protected to allow unit tests to invoke it. */
    void gc() {
        if (stopped) {
            return;
        }
        long expirationTime = System.currentTimeMillis() - timeout;
        for (Iterator<Map.Entry<InputStreamKey, TimedStreamEntry>> iter = streamMap.entrySet().iterator(); iter.hasNext();) {
            if (stopped) {
                return;
            }
            Map.Entry<InputStreamKey, TimedStreamEntry> entry = iter.next();
            TimedStreamEntry timedStreamEntry = entry.getValue();
            if (timedStreamEntry.timestamp.get() <= expirationTime) {
                iter.remove();
                InputStreamKey key = entry.getKey();
                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (timedStreamEntry) { // ensure there's no race with a request that got a ref before we removed it
                    closeStreamEntry(timedStreamEntry, key.requestId, key.index);
                }
            }
        }
    }

    private static void closeStreamEntry(Closeable closeable, int requestId, int streamIndex) {
        try {
            closeable.close();
        } catch (IOException e) {
            ControllerLogger.ROOT_LOGGER.debugf(e, "Caught exception closing attached response stream at index %d for operation %d", streamIndex, requestId);
        }
    }

    /**
     * Key encapsulating an operation id and the index of a stream attached to the operation response.
     */
    private static class InputStreamKey {
        private final int requestId;
        private final int index;

        InputStreamKey(int requestId, int index) {
            this.requestId = requestId;
            this.index = index;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            InputStreamKey that = (InputStreamKey) o;

            return index == that.index && requestId == that.requestId;

        }

        @Override
        public int hashCode() {
            int result = requestId;
            result = 31 * result + index;
            return result;
        }
    }

    private abstract class AbstractAttachmentHandler implements ManagementRequestHandler<Void, Void> {

        @Override
        public void handleRequest(final DataInput input, final ActiveOperation.ResultHandler<Void> resultHandler,
                                  final ManagementRequestContext<Void> context) throws IOException {
            // Read the inputStream key
            expectHeader(input, ModelControllerProtocol.PARAM_OPERATION);
            final int requestId = input.readInt();
            expectHeader(input, ModelControllerProtocol.PARAM_INPUTSTREAM_INDEX);
            final int index = input.readInt();
            final InputStreamKey key = new InputStreamKey(requestId, index);
            context.executeAsync(new ManagementRequestContext.AsyncTask<Void>() {
                @Override
                public void execute(final ManagementRequestContext<Void> context) throws Exception {
                    final ManagementRequestHeader header = ManagementRequestHeader.class.cast(context.getRequestHeader());
                    final ManagementResponseHeader response = new ManagementResponseHeader(header.getVersion(), header.getRequestId(), null);
                    final TimedStreamEntry entry = streamMap.remove(key);  // remove as we'll never use it again

                    FlushableDataOutput output = null;
                    try {
                        output = context.writeMessage(response);
                        if (entry == null) {
                            // Either a bogus request or a request for a stream that has timed out
                            // and been cleaned up.
                            handleMissingStream(requestId, index, output);
                        } else {
                            //noinspection SynchronizationOnLocalVariableOrMethodParameter
                            synchronized (entry) { // lock out any gc work
                                if (entry.closed) {
                                    // Just cleaned up
                                    handleMissingStream(requestId, index, output);
                                } else {
                                    handleRequest(entry, output);
                                    entry.timestamp.set(System.currentTimeMillis());
                                }
                            }
                        }
                        output.writeByte(ManagementProtocol.RESPONSE_END);
                        output.close();
                        resultHandler.done(null);
                    } finally {
                        StreamUtils.safeClose(output);
                        StreamUtils.safeClose(entry);
                    }
                }
            });
        }

        abstract void handleRequest(final TimedStreamEntry entry, final FlushableDataOutput output) throws IOException;

        abstract void handleMissingStream(int requestId, int index, final FlushableDataOutput output) throws IOException;
    }

    private class ReadHandler extends AbstractAttachmentHandler {
        private static final int BUFFER_SIZE = 8192;

        @Override
        void handleRequest(TimedStreamEntry entry, FlushableDataOutput output) throws IOException {

            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (entry) {
                InputStream input = entry.streamEntry.getStream();
                int read = 0;
                byte[] buffer = new byte[BUFFER_SIZE];
                do {
                    // Set the timestamp on each loop so if there are blocking delays reading or writing
                    // they don't accumulate
                    entry.timestamp.set(System.currentTimeMillis());

                    int totalRead = 0;
                    int remaining = BUFFER_SIZE;
                    // Read a full buffer if possible before sending
                    while (remaining > 0 && (read = input.read(buffer, totalRead, remaining)) != -1) {
                        totalRead += read;
                        remaining -= read;
                    }
                    if (totalRead > 0) {
                        output.writeByte(ModelControllerProtocol.PARAM_INPUTSTREAM_LENGTH);
                        output.writeInt(totalRead);
                        output.writeByte(ModelControllerProtocol.PARAM_INPUTSTREAM_CONTENTS);
                        output.write(buffer, 0, totalRead);
                    }

                } while (read > -1);

                output.writeByte(ModelControllerProtocol.PARAM_END);
            }
        }

        @Override
        void handleMissingStream(int requestId, int index, FlushableDataOutput output) throws IOException {
            // Respond as if stream was empty
            ControllerLogger.MGMT_OP_LOGGER.debugf("Received request for unavailable stream at index %d for request id %d; responding with EOF", index, requestId);
            output.write(ModelControllerProtocol.PARAM_END);
        }
    }

    private static class TimedStreamEntry implements Closeable {
        private final OperationResponse.StreamEntry streamEntry;
        private final AtomicLong timestamp;
        private boolean closed;

        private TimedStreamEntry(OperationResponse.StreamEntry streamEntry, AtomicLong timestamp) {
            this.streamEntry = streamEntry;
            this.timestamp = timestamp;
        }

        @Override
        public void close() throws IOException {
            streamEntry.close();
            closed = true;
        }
    }

    private class CleanupTask implements Runnable {

        @Override
        public void run() {
            ResponseAttachmentInputStreamSupport.this.gc();
        }
    }
}
