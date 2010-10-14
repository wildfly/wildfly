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

package org.jboss.as.domain.client.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

import org.jboss.as.domain.client.api.DomainUpdateApplier;
import org.jboss.as.domain.client.api.ServerIdentity;
import org.jboss.as.domain.client.api.DomainUpdateApplier.Context;
import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.UpdateResultHandler;

/**
 * Implementation of the {@link DomainUpdateApplier} {@link Context} interface
 * that delegates {@link DomainClientImpl} for invocations on a remote
 * domain controller.
 *
 * @author Brian Stansberry
 */
class DomainUpdateApplierContextImpl<R> implements Context<R> {

    private final List<ServerIdentity> servers = new ArrayList<ServerIdentity>();
    private final DomainClientImpl client;
    private final AbstractServerModelUpdate<R> update;
    private Future<UpdateResultHandlerResponse<R>> responseFuture;
    private boolean cancelled;

    static <R> Context<R> createDomainUpdateApplierContext(final DomainClientImpl client,
            final List<ServerIdentity> servers, final AbstractServerModelUpdate<R> update) {
        return new DomainUpdateApplierContextImpl<R>(client, servers, update);
    }

    DomainUpdateApplierContextImpl(final DomainClientImpl client,
                                   final List<ServerIdentity> servers,
                                   final AbstractServerModelUpdate<R> update) {
        assert client != null : "client is null";
        assert update != null : "update is null";
        this.client = client;
        this.update = update;
        if (servers != null) {
            this.servers.addAll(servers);
        }
    }

    @Override
    public <P> void apply(ServerIdentity server, UpdateResultHandler<R, P> resultHandler, P param) {
        if (!servers.contains(server)) {
            throw new IllegalArgumentException("Unknown server " + server);
        }
        synchronized (this) {
            if (!cancelled) {
                responseFuture = client.applyUpdateToServer(update, server);
            }
        }
    }

    @Override
    public void cancel() {
        synchronized (this) {
            cancelled = true;
            if (responseFuture != null) {
                responseFuture.cancel(true);
            }
        }
    }

    @Override
    public Collection<ServerIdentity> getAffectedServers() {
        return Collections.unmodifiableList(servers);
    }

}
