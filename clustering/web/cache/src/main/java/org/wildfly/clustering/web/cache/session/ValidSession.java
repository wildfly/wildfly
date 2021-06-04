/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
        this.validate();
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
