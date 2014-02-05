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

package org.wildfly.clustering.web.infinispan.sso.coarse;

import org.jboss.as.clustering.infinispan.invoker.Mutator;
import org.wildfly.clustering.web.sso.Authentication;
import org.wildfly.clustering.web.sso.AuthenticationType;

/**
 * @author Paul Ferraro
 */
public class CoarseAuthentication<I> implements Authentication<I> {

    private final CoarseAuthenticationEntry<I, ?, ?> entry;
    private final Mutator mutator;

    public CoarseAuthentication(CoarseAuthenticationEntry<I, ?, ?> entry, Mutator mutator) {
        this.entry = entry;
        this.mutator = mutator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public I getIdentity() {
        return this.entry.getIdentity();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setIdentity(I identity) {
        this.entry.setIdentity(identity);
        this.mutator.mutate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AuthenticationType getType() {
        return this.entry.getType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setType(AuthenticationType type) {
        this.entry.setAuthenticationType(type);
        this.mutator.mutate();
    }
}
