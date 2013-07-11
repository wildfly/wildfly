package org.jboss.as.clustering.management.support.impl;

import java.io.Serializable;

import org.jboss.as.clustering.ClusterNode;

/**
 * Represents the state of a remote cluster peer for the purposes
 * of displaying an abbreviated summarised state.
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public class RemoteClusterResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    public final ClusterNode responder;
    private String view = null ;
    private String view_history = null ;
    private int async_unicasts = 0;
    private int sync_unicasts = 0;
    private int async_multicasts = 0;
    private int sync_multicasts = 0;
    private int async_anycasts = 0;
    private int sync_anycasts = 0;

    RemoteClusterResponse(ClusterNode responder) {
        this.responder = responder;
    }

    public ClusterNode getResponder() {
        return responder;
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

    public int getAsyncUnicasts() {
        return async_unicasts;
    }

    public void setAsyncUnicasts(int async_unicasts) {
        this.async_unicasts = async_unicasts;
    }

    public int getSyncUnicasts() {
        return sync_unicasts;
    }

    public void setSyncUnicasts(int sync_unicasts) {
        this.sync_unicasts = sync_unicasts;
    }

    public int getAsyncMulticasts() {
        return async_multicasts;
    }

    public void setAsyncMulticasts(int async_multicasts) {
        this.async_multicasts = async_multicasts;
    }

    public int getSyncMulticasts() {
        return sync_multicasts;
    }

    public void setSyncMulticasts(int sync_multicasts) {
        this.sync_multicasts = sync_multicasts;
    }

    public int getAsyncAnycasts() {
        return async_anycasts;
    }

    public void setAsyncAnycasts(int async_anycasts) {
        this.async_anycasts = async_anycasts;
    }

    public int getSyncAnycasts() {
        return sync_anycasts;
    }

    public void setSyncAnycasts(int sync_anycasts) {
        this.sync_anycasts = sync_anycasts;
    }
}
