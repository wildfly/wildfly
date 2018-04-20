/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.server.dispatcher;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionStage;

import org.jgroups.Address;
import org.jgroups.blocks.RequestCorrelator;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.UnicastRequest;
import org.jgroups.util.Buffer;
import org.wildfly.clustering.dispatcher.CommandDispatcherException;

/**
 * Translates {@link NoSuchService} response to a {@link CancellationException}.
 * @author Paul Ferraro
 */
public class ServiceRequest<T> extends UnicastRequest<T> {

    public ServiceRequest(RequestCorrelator correlator, Address target, RequestOptions options) {
        super(correlator, target, options);
    }

    public CompletionStage<T> send(Buffer data) throws CommandDispatcherException {
        try {
            this.sendRequest(data);
            return this;
        } catch (Exception e) {
            throw new CommandDispatcherException(e);
        }
    }

    @Override
    public boolean complete(T value) {
        return (value instanceof NoSuchService) ? this.completeExceptionally(new CancellationException()) : super.complete(value);
    }
}
