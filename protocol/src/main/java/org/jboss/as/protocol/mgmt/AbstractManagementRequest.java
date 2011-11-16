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

package org.jboss.as.protocol.mgmt;

import org.jboss.as.protocol.StreamUtils;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.MessageOutputStream;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * utility class for creating management requests.
 *
 * @param <T> the response type
 * @param <A> the attachment type
 *
 * @author Emanuel Muckenhuber
 */
public abstract class AbstractManagementRequest<T, A> implements ManagementRequest<T, A> {

    /**
     * Send the request.
     *
     * @param resultHandler the result handler
     * @param context the request context
     * @param output the data output
     * @throws java.io.IOException
     */
    protected abstract void sendRequest(ActiveOperation.ResultHandler<T> resultHandler, ManagementRequestContext<A> context, FlushableDataOutput output) throws IOException;

    @Override
    public void sendRequest(final ActiveOperation.ResultHandler<T> resultHandler, final ManagementRequestContext<A> context) throws IOException {
        context.executeAsync(new ManagementRequestContext.AsyncTask<A>() {
            @Override
            public void execute(final ManagementRequestContext<A> context) throws Exception {
                final FlushableDataOutput output = context.writeMessage(context.getRequestHeader());
                try {
                    sendRequest(resultHandler, context, output);
                    output.writeByte(ManagementProtocol.REQUEST_END);
                    output.close();
                } finally {
                    StreamUtils.safeClose(output);
                }
            }
        });
    }

    @Deprecated
    public T executeForResult(final AbstractMessageHandler<T, A> handler, final Channel channel, final A attachment) throws ExecutionException, InterruptedException {
        final ActiveOperation<T, A> support = handler.registerActiveOperation(attachment);
        return handler.executeRequest(this, channel, support).get();
    }

}
