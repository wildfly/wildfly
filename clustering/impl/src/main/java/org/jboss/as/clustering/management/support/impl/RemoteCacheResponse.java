package org.jboss.as.clustering.management.support.impl;

import java.io.Serializable;

import org.jboss.as.clustering.ClusterNode;

/**
 * Represents the state of a remote cache peer for the purposes
 * of displaying an abbreviated summarised state.
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public class RemoteCacheResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    public final ClusterNode responder;
    private boolean inView = false;
    private String view = null ;
    private String view_history = null ;
    private long hits = 0L;
    private long misses = 0L;
    private long stores = 0L;
    private long remove_hits = 0L;
    private long remove_misses = 0L;
    private long commits = 0L;
    private long prepares = 0L;
    private long rollbacks = 0L;
    private long entries = 0L;
    private long rpc_count = 0L;
    private long rpc_failures = 0L;

    RemoteCacheResponse(ClusterNode responder) {
        this.responder = responder;
    }

    public ClusterNode getResponder() {
        return responder;
    }

    public boolean isInView() {
        return inView;
    }

    public void setInView(boolean inView) {
        this.inView = inView;
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }

    public String getViewHistory() {
        return view_history;
    }

    public void setViewHistory(String view_history) {
        this.view_history = view_history;
    }

    public long getHits() {
        return hits;
    }

    public void setHits(long hits) {
        this.hits = hits;
    }

    public long getMisses() {
        return misses;
    }

    public void setMisses(long misses) {
        this.misses = misses;
    }

    public long getStores() {
        return stores;
    }

    public void setStores(long stores) {
        this.stores = stores;
    }

    public long getRemoveHits() {
        return remove_hits;
    }

    public void setRemoveHits(long remove_hits) {
        this.remove_hits = remove_hits;
    }

    public long getRemoveMisses() {
        return remove_misses;
    }

    public void setRemoveMisses(long remove_misses) {
        this.remove_misses = remove_misses;
    }

    public long getCommits() {
        return commits;
    }

    public void setCommits(long commits) {
        this.commits = commits;
    }

    public long getPrepares() {
        return prepares;
    }

    public void setPrepares(long prepares) {
        this.prepares = prepares;
    }

    public long getRollbacks() {
        return rollbacks;
    }

    public void setRollbacks(long rollbacks) {
        this.rollbacks = rollbacks;
    }

    public long getEntries() {
        return entries;
    }

    public void setEntries(long entries) {
        this.entries = entries;
    }

    public long getRPCCount() {
        return rpc_count;
    }

    public void setRPCCount(long rpc_count) {
        this.rpc_count = rpc_count;
    }

    public long getRPCFailures() {
        return rpc_failures;
    }

    public void setRPCFailures(long rpc_failures) {
        this.rpc_failures = rpc_failures;
    }
}
