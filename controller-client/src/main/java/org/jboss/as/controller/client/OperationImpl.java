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

import org.jboss.as.protocol.StreamUtils;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.controller.client.ControllerClientMessages.MESSAGES;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class OperationImpl implements Operation {

    private final boolean autoCloseStreams;
    private final ModelNode operation;
    private final List<InputStream> inputStreams;

    OperationImpl(final ModelNode operation, final List<InputStream> inputStreams) {
        this(operation, inputStreams, false);
    }

    OperationImpl(final ModelNode operation, final List<InputStream> inputStreams, final boolean autoCloseStreams) {
        this.operation = operation;
        this.inputStreams = inputStreams;
        this.autoCloseStreams = autoCloseStreams;
    }

    @Override
    public boolean isAutoCloseStreams() {
        return autoCloseStreams;
    }

    @Override
    public ModelNode getOperation() {
        return operation;
    }

    @Override
    public List<InputStream> getInputStreams() {
        if (inputStreams == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(inputStreams);
    }

    @Override
    @Deprecated
    public Operation clone() {
        List<InputStream> streamsCopy = inputStreams == null ? null : new ArrayList<InputStream>(inputStreams);
        return new OperationImpl(operation.clone(), streamsCopy);
    }

    @Override
    @Deprecated
    public Operation clone(final ModelNode operation) {
        if (operation == null) {
            throw MESSAGES.nullVar("operation");
        }
        List<InputStream> streamsCopy = inputStreams == null ? null : new ArrayList<InputStream>(inputStreams);
        return new OperationImpl(operation, streamsCopy);
    }

    @Override
    public void close() throws IOException {
        final List<InputStream> streams = getInputStreams();
        for(final InputStream stream : streams) {
            StreamUtils.safeClose(stream);
        }
    }
}
