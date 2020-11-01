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

package org.jboss.as.test.clustering.cluster.web.event;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionEvent;

import net.jcip.annotations.Immutable;

/**
 * @author Paul Ferraro
 */
@WebServlet(urlPatterns = { SessionActivationServlet.SERVLET_PATH })
public class SessionActivationServlet extends HttpServlet {
    private static final long serialVersionUID = 1000811377717375782L;
    private static final String SERVLET_NAME = "activation";
    static final String SERVLET_PATH = "/" + SERVLET_NAME;

    private static final String IMMUTABLE_ATTRIBUTE_NAME = "immutable";
    private static final String MUTABLE_ATTRIBUTE_NAME = "mutable";

    public static URI createURI(URL baseURL) throws URISyntaxException {
        return createURI(baseURL.toURI());
    }

    public static URI createURI(URI baseURI) {
        return baseURI.resolve(SERVLET_NAME);
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getServletContext().log(String.format("[%s] %s", request.getMethod(), request.getRequestURI()));
        super.service(request, response);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        SessionActivationListener listener = new ImmutableSessionActivationListener(true);
        session.setAttribute(IMMUTABLE_ATTRIBUTE_NAME, listener);
        listener.assertActive();
        listener = new MutableSessionActivationListener(true);
        session.setAttribute(MUTABLE_ATTRIBUTE_NAME, listener);
        listener.assertActive();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        ((SessionActivationListener) session.getAttribute(IMMUTABLE_ATTRIBUTE_NAME)).assertActive();
        ((SessionActivationListener) session.getAttribute(MUTABLE_ATTRIBUTE_NAME)).assertActive();
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        session.removeAttribute(IMMUTABLE_ATTRIBUTE_NAME);
        session.removeAttribute(MUTABLE_ATTRIBUTE_NAME);
    }

    private static class MutableSessionActivationListener extends SessionActivationListener {
        private static final long serialVersionUID = 7944013938798510317L;

        MutableSessionActivationListener(boolean active) {
            super(active);
        }
    }

    @Immutable
    private static class ImmutableSessionActivationListener extends SessionActivationListener {
        private static final long serialVersionUID = -6294242158180373273L;

        ImmutableSessionActivationListener(boolean active) {
            super(active);
        }
    }

    private abstract static class SessionActivationListener implements HttpSessionActivationListener, HttpSessionBindingListener, Serializable {
        private static final long serialVersionUID = 8262547876438811845L;

        private transient boolean active = false;

        SessionActivationListener(boolean active) {
            this.active = active;
        }

        public void assertActive() {
            if (!this.active) {
                throw new IllegalStateException(String.format("%s.sessionDidActivate(...) not invoked", this.getClass().getSimpleName()));
            }
        }

        @Override
        public void sessionWillPassivate(HttpSessionEvent event) {
            if (!this.active) {
                throw new IllegalStateException(String.format("%s.sessionWillPassivate(...) already invoked", this.getClass().getSimpleName()));
            }
            this.active = false;
        }

        @Override
        public void sessionDidActivate(HttpSessionEvent event) {
            if (this.active) {
                throw new IllegalStateException(String.format("%s.sessionDidActivate(...) already invoked", this.getClass().getSimpleName()));
            }
            this.active = true;
        }

        @Override
        public void valueBound(HttpSessionBindingEvent event) {
            this.assertActive();
        }

        @Override
        public void valueUnbound(HttpSessionBindingEvent event) {
            this.assertActive();
        }

        private void writeObject(java.io.ObjectOutputStream out) throws IOException {
            if (this.active) {
                throw new NotSerializableException(String.format("%s.sessionWillPassivate(...) not invoked", this.getClass().getSimpleName()));
            }
            out.defaultWriteObject();
        }
    }
}
