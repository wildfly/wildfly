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
package org.jboss.as.controller.client;

import org.jboss.dmr.ModelNode;

import static org.jboss.as.controller.client.ControllerClientMessages.MESSAGES;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder for a {@link Operation}.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class OperationBuilder {

    private final ModelNode operation;
    private volatile List<InputStream> inputStreams;
    private boolean autoCloseStreams = false;

    public OperationBuilder(final ModelNode operation) {
        this(operation, false);
    }

    public OperationBuilder(final ModelNode operation, boolean autoCloseStreams) {
        if (operation == null) {
            throw MESSAGES.nullVar("operation");
        }
        this.operation = operation;
        this.autoCloseStreams = autoCloseStreams;
    }

    /**
     * Associate an input stream with the operation. Closing the input stream
     * is the responsibility of the caller.
     *
     * @param in  the input stream. Cannot be {@code null}
     * @return a builder than can be used to continue building the operation
     */
    public OperationBuilder addInputStream(final InputStream in) {
        if(in == null) {
            throw MESSAGES.nullVar("input-stream");
        }
        if (inputStreams == null) {
            inputStreams = new ArrayList<InputStream>();
        }
        inputStreams.add(in);
        return this;
    }

    /**
     * Gets the number of input streams currently associated with the operation,
     *
     * @return  the number of input streams
     */
    public int getInputStreamCount() {
        List<InputStream> list = inputStreams;
        return list == null ? 0 : list.size();
    }

    /**
     * Automatically try to close the stream, once the operation finished executing.
     *
     * @param autoCloseStreams whether to close the streams or not
     */
    public void setAutoCloseStreams(boolean autoCloseStreams) {
        this.autoCloseStreams = autoCloseStreams;
    }

    /**
     * Builds the operation.
     *
     * @return the operation
     */
    public Operation build() {
        return new OperationImpl(operation, inputStreams, autoCloseStreams);
    }

    /**
     * Create an operation builder.
     *
     * @param operation the operation
     * @return the builder
     */
    public static OperationBuilder create(final ModelNode operation) {
        return new OperationBuilder(operation);
    }

    /**
     * Create an operation builder.
     *
     * @param operation the operation
     * @param autoCloseStreams whether streams should be automatically closed
     * @return the builder
     */
    public static OperationBuilder create(final ModelNode operation, final boolean autoCloseStreams) {
        return new OperationBuilder(operation, autoCloseStreams);
    }

}
