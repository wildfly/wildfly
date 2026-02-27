/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.session;

import java.util.function.Function;

import org.wildfly.clustering.server.util.Reference;

import io.undertow.server.session.Session;
import io.undertow.server.session.SessionManager;

/**
 * A reference to an Undertow session.
 * @author Paul Ferraro
 */
public class DistributableSessionReference implements SessionReference, Reference.Reader<Session> {
    private final SessionManager manager;
    private final String id;

    public DistributableSessionReference(SessionManager manager, String id) {
        this.manager = manager;
        this.id = id;
    }

    @Override
    public SessionManager getManager() {
        return this.manager;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public Reader<Session> getReader() {
        return this;
    }

    @Override
    public <R> R read(Function<Session, R> reader) {
        Session session = this.manager.getSession(null, new SimpleSessionConfig(this.getId()));
        if (session == null) {
            throw new IllegalStateException();
        }
        try {
            return reader.apply(session);
        } finally {
            session.requestDone(null);
        }
    }
}
