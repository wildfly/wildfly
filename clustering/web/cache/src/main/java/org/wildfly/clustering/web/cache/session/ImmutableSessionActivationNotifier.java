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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionEvent;

import org.wildfly.clustering.web.session.ImmutableSession;

/**
 * Triggers activation events for all attributes of a session.
 * @author Paul Ferraro
 */
public class ImmutableSessionActivationNotifier implements SessionActivationNotifier {

    private final FilteringHttpSession session;

    public ImmutableSessionActivationNotifier(ImmutableSession session, ServletContext context) {
        this.session = new ImmutableFilteringHttpSession(session, context);
    }

    @Override
    public void prePassivate() {
        Map<String, HttpSessionActivationListener> listeners = this.session.getAttributes(HttpSessionActivationListener.class);
        if (!listeners.isEmpty()) {
            HttpSessionEvent event = new HttpSessionEvent(this.session);
            for (HttpSessionActivationListener listener : listeners.values()) {
                listener.sessionWillPassivate(event);
            }
        }
    }

    @Override
    public void postActivate() {
        Map<String, HttpSessionActivationListener> listeners = this.session.getAttributes(HttpSessionActivationListener.class);
        if (!listeners.isEmpty()) {
            HttpSessionEvent event = new HttpSessionEvent(this.session);
            for (HttpSessionActivationListener listener : listeners.values()) {
                listener.sessionDidActivate(event);
            }
        }
    }
}
