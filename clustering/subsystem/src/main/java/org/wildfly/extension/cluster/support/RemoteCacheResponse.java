package org.wildfly.extension.cluster.support;

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
    private String view = null ;

    RemoteCacheResponse(ClusterNode responder) {
        this.responder = responder;
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }
}
