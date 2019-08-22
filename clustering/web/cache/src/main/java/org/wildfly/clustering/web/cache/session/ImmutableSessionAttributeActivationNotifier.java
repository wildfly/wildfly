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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionEvent;

import org.wildfly.clustering.web.session.ImmutableSession;

/**
 * @author Paul Ferraro
 */
public class ImmutableSessionAttributeActivationNotifier implements SessionAttributeActivationNotifier {
    private static final Function<Supplier<HttpSessionActivationListener>, HttpSessionActivationListener> PRE_PASSIVATE_LISTENER_FACTORY = new HttpSessionActivationListenerFactory(true);
    private static final Function<Supplier<HttpSessionActivationListener>, HttpSessionActivationListener> POST_ACTIVATE_LISTENER_FACTORY = new HttpSessionActivationListenerFactory(false);

    private final HttpSessionEvent event;
    private final Map<Supplier<HttpSessionActivationListener>, HttpSessionActivationListener> listeners = new ConcurrentHashMap<>();

    public ImmutableSessionAttributeActivationNotifier(ImmutableSession session, ServletContext context) {
        this(new ImmutableFilteringHttpSession(session, context));
    }

    public ImmutableSessionAttributeActivationNotifier(FilteringHttpSession session) {
        this.event = new HttpSessionEvent(session);
    }

    @Override
    public void prePassivate(Object object) {
        if (object instanceof HttpSessionActivationListener) {
            Supplier<HttpSessionActivationListener> reference = new HttpSessionActivationListenerKey((HttpSessionActivationListener) object);
            HttpSessionActivationListener listener = this.listeners.computeIfAbsent(reference, PRE_PASSIVATE_LISTENER_FACTORY);
            listener.sessionWillPassivate(this.event);
        }
    }

    @Override
    public void postActivate(Object object) {
        if (object instanceof HttpSessionActivationListener) {
            Supplier<HttpSessionActivationListener> reference = new HttpSessionActivationListenerKey((HttpSessionActivationListener) object);
            HttpSessionActivationListener listener = this.listeners.computeIfAbsent(reference, POST_ACTIVATE_LISTENER_FACTORY);
            listener.sessionDidActivate(this.event);
        }
    }

    @Override
    public void close() {
        for (HttpSessionActivationListener listener : this.listeners.values()) {
            listener.sessionWillPassivate(this.event);
        }
        this.listeners.clear();
    }

    /**
     * Map key of a {@link HttpSessionActivationListener} that uses identity equality.
     */
    private static class HttpSessionActivationListenerKey implements Supplier<HttpSessionActivationListener> {
        private final HttpSessionActivationListener listener;

        HttpSessionActivationListenerKey(HttpSessionActivationListener listener) {
            this.listener = listener;
        }

        @Override
        public HttpSessionActivationListener get() {
            return this.listener;
        }

        @Override
        public int hashCode() {
            return this.listener.hashCode();
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof HttpSessionActivationListenerKey)) return false;
            HttpSessionActivationListenerKey reference = (HttpSessionActivationListenerKey) object;
            return this.listener == reference.listener;
        }
    }

    /**
     * Factory for creating HttpSessionActivationListener values.
     */
    private static class HttpSessionActivationListenerFactory implements Function<Supplier<HttpSessionActivationListener>, HttpSessionActivationListener> {
        private final boolean active;

        HttpSessionActivationListenerFactory(boolean active) {
            this.active = active;
        }

        @Override
        public HttpSessionActivationListener apply(Supplier<HttpSessionActivationListener> reference) {
            return new AtomicHttpSessionActivationListener(reference.get(), this.active);
        }
    }

    /**
     * Prevents redundant session activation events for a given listener.
     */
    private static class AtomicHttpSessionActivationListener implements HttpSessionActivationListener {
        private final HttpSessionActivationListener listener;
        private final AtomicBoolean active;

        AtomicHttpSessionActivationListener(HttpSessionActivationListener listener, boolean active) {
            this.listener = listener;
            this.active = new AtomicBoolean(active);
        }

        @Override
        public void sessionWillPassivate(HttpSessionEvent event) {
            if (this.active.compareAndSet(true, false)) {
                this.listener.sessionWillPassivate(event);
            }
        }

        @Override
        public void sessionDidActivate(HttpSessionEvent event) {
            if (this.active.compareAndSet(false, true)) {
                this.listener.sessionDidActivate(event);
            }
        }
    }
}
