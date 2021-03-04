/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

import java.util.concurrent.atomic.AtomicBoolean;

import org.wildfly.clustering.web.session.HttpSessionActivationListenerProvider;
import org.wildfly.clustering.web.session.ImmutableSession;

/**
 * Triggers activation/passivation events for all attributes of a persistent session.
 * Ensures duplicate events are not triggered.
 * @param <S> the HttpSession specification type
 * @param <C> the ServletContext specification type
 * @param <L> the HttpSessionActivationListener specification type
 * @author Paul Ferraro
 */
public class PersistentImmutableSessionActivationNotifier<S, C, L> extends ImmutableSessionActivationNotifier<S, C, L> {

    private final AtomicBoolean active = new AtomicBoolean(false);

    public PersistentImmutableSessionActivationNotifier(HttpSessionActivationListenerProvider<S, C, L> provider, ImmutableSession session, C context) {
        super(provider, session, context);
    }

    PersistentImmutableSessionActivationNotifier(HttpSessionActivationListenerProvider<S, C, L> provider, ImmutableSession session, C context, SessionAttributesFilter filter) {
        super(provider, session, context, filter);
    }

    @Override
    public void prePassivate() {
        if (this.active.compareAndSet(true, false)) {
            super.prePassivate();
        }
    }

    @Override
    public void postActivate() {
        if (this.active.compareAndSet(false, true)) {
            super.postActivate();
        }
    }
}
