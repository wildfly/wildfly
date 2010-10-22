/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server.manager.management;

import java.io.IOException;
import java.io.InputStream;

import org.jboss.as.protocol.Connection;
import org.jboss.as.protocol.MessageHandler;

/**
 * A {@link ManagementOperationHandler} that delegates all {@link MessageHandler}
 * operations.
 *
 * @author Brian Stansberry
 */
public class DelegatingManagementOperationHandler implements ManagementOperationHandler {

    private final byte identifier;
    private final MessageHandler delegate;

    public DelegatingManagementOperationHandler(final MessageHandler delegate, final byte identifier) {
        assert delegate != null : "delegate is null";
        this.delegate = delegate;
        this.identifier = identifier;
    }

    @Override
    public byte getIdentifier() {
        return identifier;
    }

    @Override
    public void handleFailure(Connection connection, IOException e) throws IOException {
        delegate.handleFailure(connection, e);
    }

    @Override
    public void handleFinished(Connection connection) throws IOException {
        delegate.handleFinished(connection);
    }

    @Override
    public void handleMessage(Connection connection, InputStream dataStream) throws IOException {
        delegate.handleMessage(connection, dataStream);
    }

    @Override
    public void handleShutdown(Connection connection) throws IOException {
        delegate.handleShutdown(connection);
    }

}
