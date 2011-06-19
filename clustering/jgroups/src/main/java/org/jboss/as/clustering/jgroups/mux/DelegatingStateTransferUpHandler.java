/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc. and individual contributors
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

package org.jboss.as.clustering.jgroups.mux;

import org.jgroups.Event;
import org.jgroups.UpHandler;

/**
 * An UpHandler that integrates another UpHandler and a StateTransferFilter.
 *
 * @author Brian Stansberry
 *
 * @version $Revision$
 */
public class DelegatingStateTransferUpHandler implements StateTransferFilter, UpHandler {
    private final UpHandler delegate;
    private final StateTransferFilter filter;

    /**
     * Create a new DelegatingStateTransferUpHandler.
     *
     * @param delegate the UpHandler to delegate to
     * @param filter the StateTransferFilter to delegate to
     */
    public DelegatingStateTransferUpHandler(UpHandler delegate, StateTransferFilter filter) {
        assert delegate != null : "delegate is null";
        assert filter != null : "filter is null";

        this.delegate = delegate;
        this.filter = filter;
    }

    /**
     * Passes the event to the delegate UpHandler.
     *
     * {@inheritDoc}
     */
    @Override
    public Object up(Event evt) {
        return delegate.up(evt);
    }

    /**
     * Checks with the delegate StateTransferFilter.
     *
     * {@inheritDoc}
     */
    @Override
    public boolean accepts(String stateId) {
        return filter.accepts(stateId);
    }
}
