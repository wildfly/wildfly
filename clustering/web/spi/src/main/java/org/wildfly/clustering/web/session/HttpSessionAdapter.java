/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.web.session;

import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

/**
 * Adapts a {@link Session} to the {@link HttpSession} interface invoking the appropriate events
 * on {@link #setAttribute(String, Object)} and {@link #removeAttribute(String)}.
 * @author Paul Ferraro
 */
public class HttpSessionAdapter extends ImmutableHttpSessionAdapter {

    private final Session<?> session;

    public HttpSessionAdapter(Session<?> session) {
        super(session);
        this.session = session;
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        this.session.getMetaData().setMaxInactiveInterval(interval, TimeUnit.SECONDS);
    }

    @Override
    public void setAttribute(String name, Object value) {
        Object oldValue = this.session.getAttributes().setAttribute(name, value);

        if (value != oldValue) {
            HttpSessionBindingEvent event = new HttpSessionBindingEvent(this, name, (oldValue != null) ? oldValue : value);
            for (HttpSessionAttributeListener listener: this.session.getContext().getSessionAttributeListeners()) {
                if (oldValue == null) {
                    listener.attributeAdded(event);
                } else if (value == null) {
                    listener.attributeRemoved(event);
                } else {
                    listener.attributeReplaced(event);
                }
            }

            if (oldValue instanceof HttpSessionBindingListener) {
                HttpSessionBindingListener listener = (HttpSessionBindingListener) oldValue;
                listener.valueUnbound(new HttpSessionBindingEvent(this, name));
            }
            if (value instanceof HttpSessionBindingListener) {
                HttpSessionBindingListener listener = (HttpSessionBindingListener) value;
                listener.valueBound(new HttpSessionBindingEvent(this, name));
            }
        }
    }

    @Override
    public void removeAttribute(String name) {
        Object value = this.session.getAttributes().removeAttribute(name);

        if (value != null) {
            HttpSessionBindingEvent event = new HttpSessionBindingEvent(this, name, value);
            for (HttpSessionAttributeListener listener: this.session.getContext().getSessionAttributeListeners()) {
                listener.attributeRemoved(event);
            }

            if (value instanceof HttpSessionBindingListener) {
                HttpSessionBindingListener listener = (HttpSessionBindingListener) value;
                listener.valueUnbound(new HttpSessionBindingEvent(this, name));
            }
        }
    }

    @Override
    public void invalidate() {
        this.session.invalidate();
    }
}
