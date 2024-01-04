/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session;

import java.util.function.Consumer;

import org.wildfly.clustering.web.cache.logging.Logger;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionAttributes;
import org.wildfly.clustering.web.session.SessionMetaData;

/**
 * {@link Session} decorator whose methods throw an {@link IllegalStateException} if the session is not valid.
 * @author Paul Ferraro
 */
public class ValidSession<L> implements Session<L> {
    private final Session<L> session;
    private final Consumer<ImmutableSession> closeTask;

    public ValidSession(Session<L> session, Consumer<ImmutableSession> closeTask) {
        this.session = session;
        this.closeTask = closeTask;
    }

    private void validate() {
        if (!this.session.isValid()) {
            throw Logger.ROOT_LOGGER.invalidSession(this.getId());
        }
    }

    @Override
    public String getId() {
        return this.session.getId();
    }

    @Override
    public boolean isValid() {
        return this.session.isValid();
    }

    @Override
    public L getLocalContext() {
        return this.session.getLocalContext();
    }

    @Override
    public SessionMetaData getMetaData() {
        this.validate();
        return this.session.getMetaData();
    }

    @Override
    public SessionAttributes getAttributes() {
        this.validate();
        return this.session.getAttributes();
    }

    @Override
    public void invalidate() {
        this.validate();
        this.session.invalidate();
    }

    @Override
    public void close() {
        try {
            this.session.close();
        } finally {
            this.closeTask.accept(this.session);
        }
    }
}
