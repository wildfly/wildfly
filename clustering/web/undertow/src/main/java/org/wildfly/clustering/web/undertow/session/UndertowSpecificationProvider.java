/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.session;

import java.util.Collections;
import java.util.Enumeration;
import java.util.function.Consumer;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionActivationListener;
import jakarta.servlet.http.HttpSessionEvent;

import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.SpecificationProvider;

/**
 * @author Paul Ferraro
 */
public enum UndertowSpecificationProvider implements SpecificationProvider<HttpSession, ServletContext, HttpSessionActivationListener> {
    INSTANCE;

    @Override
    public HttpSession createHttpSession(ImmutableSession session, ServletContext context) {
        return new HttpSession() {
            @Override
            public String getId() {
                return session.getId();
            }

            @Override
            public ServletContext getServletContext() {
                return context;
            }

            @Override
            public boolean isNew() {
                return session.getMetaData().isNew();
            }

            @Override
            public long getCreationTime() {
                return session.getMetaData().getCreationTime().toEpochMilli();
            }

            @Override
            public long getLastAccessedTime() {
                return session.getMetaData().getLastAccessStartTime().toEpochMilli();
            }

            @Override
            public int getMaxInactiveInterval() {
                return (int) session.getMetaData().getTimeout().getSeconds();
            }

            @Override
            public Enumeration<String> getAttributeNames() {
                return Collections.enumeration(session.getAttributes().getAttributeNames());
            }

            @Override
            public Object getAttribute(String name) {
                return session.getAttributes().getAttribute(name);
            }

            @Override
            public void setAttribute(String name, Object value) {
                // Ignore
            }

            @Override
            public void removeAttribute(String name) {
                // Ignore
            }

            @Override
            public void invalidate() {
                // Ignore
            }

            @Override
            public void setMaxInactiveInterval(int interval) {
                // Ignore
            }

            @Override
            public int hashCode() {
                return this.getId().hashCode();
            }

            @Override
            public boolean equals(Object object) {
                if (!(object instanceof HttpSession)) return false;
                // To be consistent with io.undertow.servlet.spec.HttpSessionImpl, we will assume these sessions share the same servlet context
                return this.getId().equals(((HttpSession) object).getId());
            }

            @Override
            public String toString() {
                return this.getId();
            }
        };
    }

    @Override
    public Class<HttpSessionActivationListener> getHttpSessionActivationListenerClass() {
        return HttpSessionActivationListener.class;
    }

    @Override
    public Consumer<HttpSession> prePassivateNotifier(HttpSessionActivationListener listener) {
        return new Consumer<>() {
            @Override
            public void accept(HttpSession session) {
                listener.sessionWillPassivate(new HttpSessionEvent(session));
            }
        };
    }

    @Override
    public Consumer<HttpSession> postActivateNotifier(HttpSessionActivationListener listener) {
        return new Consumer<>() {
            @Override
            public void accept(HttpSession session) {
                listener.sessionDidActivate(new HttpSessionEvent(session));
            }
        };
    }

    @Override
    public HttpSessionActivationListener createListener(Consumer<HttpSession> prePassivate, Consumer<HttpSession> postActivate) {
        return new HttpSessionActivationListener() {
            @Override
            public void sessionWillPassivate(HttpSessionEvent event) {
                prePassivate.accept(event.getSession());
            }

            @Override
            public void sessionDidActivate(HttpSessionEvent event) {
                postActivate.accept(event.getSession());
            }
        };
    }
}
