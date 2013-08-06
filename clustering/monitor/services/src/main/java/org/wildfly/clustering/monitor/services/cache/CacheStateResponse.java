/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.monitor.services.cache;

import java.io.Serializable;

import org.infinispan.interceptors.CacheMgmtInterceptor;
import org.infinispan.interceptors.TxInterceptor;
import org.infinispan.remoting.rpc.RpcManagerImpl;

public class CacheStateResponse implements CacheState, Serializable {
    private static final long serialVersionUID = -3363647361214769568L;

    private final String view;
    private volatile long hits;
    private volatile long misses;
    private volatile long stores;
    private volatile long removeHits;
    private volatile long removeMisses;
    private volatile long entries;
    private volatile long prepares;
    private volatile long commits;
    private volatile long rollbacks;
    private volatile long rpcCount;
    private volatile long rpcFailures;

    public CacheStateResponse(String view) {
        this.view = view;
    }

    @Override
    public String getView() {
        return this.view;
    }

    @Override
    public long getHits() {
        return this.hits;
    }

    @Override
    public long getMisses() {
        return this.misses;
    }

    @Override
    public long getStores() {
        return this.stores;
    }

    @Override
    public long getRemoveHits() {
        return this.removeHits;
    }

    @Override
    public long getRemoveMisses() {
        return this.removeMisses;
    }

    @Override
    public long getEntries() {
        return this.entries;
    }

    void setCacheStatistics(CacheMgmtInterceptor interceptor) {
        this.hits = interceptor.getHits();
        this.misses = interceptor.getMisses();
        this.stores = interceptor.getStores();
        this.removeHits = interceptor.getRemoveHits();
        this.removeMisses = interceptor.getRemoveMisses();
        this.entries = interceptor.getNumberOfEntries();
    }

    @Override
    public long getCommits() {
        return this.commits;
    }

    @Override
    public long getPrepares() {
        return this.prepares;
    }

    @Override
    public long getRollbacks() {
        return this.rollbacks;
    }

    void setTxStatistics(TxInterceptor interceptor) {
        this.prepares = interceptor.getPrepares();
        this.commits = interceptor.getCommits();
        this.rollbacks = interceptor.getRollbacks();
    }

    @Override
    public long getRpcCount() {
        return this.rpcCount;
    }

    @Override
    public long getRpcFailures() {
        return this.rpcFailures;
    }

    void setRpcStatistics(RpcManagerImpl manager) {
        this.rpcCount = manager.getReplicationCount();
        this.rpcFailures = manager.getReplicationFailures();
    }
}