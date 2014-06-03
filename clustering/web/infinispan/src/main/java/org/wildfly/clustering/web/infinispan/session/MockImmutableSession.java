/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.infinispan.session;

import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;
import org.wildfly.clustering.web.session.SessionContext;

/**
 * {@link ImmutableSession} optimization used to avoid full session lookup
 * if all we care about is the identifier and/or meta data.
 * @author Paul Ferraro
 */
public class MockImmutableSession implements ImmutableSession {

    private final String id;
    private final ImmutableSessionMetaData metaData;

    public MockImmutableSession(String id) {
        this(id, null);
    }

    public MockImmutableSession(String id, ImmutableSessionMetaData metaData) {
        this.id = id;
        this.metaData = metaData;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return this.id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImmutableSessionMetaData getMetaData() {
        return this.metaData;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImmutableSessionAttributes getAttributes() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SessionContext getContext() {
        return null;
    }

}
