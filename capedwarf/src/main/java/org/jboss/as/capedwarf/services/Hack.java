/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.capedwarf.services;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.RequestFacade;
import org.apache.catalina.connector.Response;
import org.apache.catalina.core.ApplicationFilterChain;
import org.apache.catalina.core.ApplicationFilterFactory;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedExceptionAction;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

/**
 * Hack around to dispatch custom request from static view.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
class Hack {

    private static final HttpServletResponse NOOP = new NoopServletResponse();
    private static final Method invoke;

    static {
        try {
            final SecurityManager sm = System.getSecurityManager();
            if (sm == null) {
                invoke = getInvoke();
            } else {
                invoke = AccessController.doPrivileged(new PrivilegedExceptionAction<Method>() {
                    public Method run() throws Exception {
                        return getInvoke();
                    }
                });
            }
        } catch (Throwable t) {
            throw new IllegalArgumentException("Error finding *invoke* on ApplicationDispatcher.", t);
        }
    }

    static void invoke(final RequestDispatcher dispatcher, final HttpServletRequest delegate) throws IOException {
        final SecurityManager sm = System.getSecurityManager();
        try {
            if (sm == null) {
                invoke.invoke(dispatcher, wrap(delegate), NOOP);
            } else {
                AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                    public Object run() throws Exception {
                        invoke.invoke(dispatcher, wrap(delegate), NOOP);
                        return null;
                    }
                });
            }
        } catch (Exception e) {
            throw new IOException(e);
        }

        // check for dispatch error
        final Object attribute = delegate.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        if (attribute == null)
            return;

        if (attribute instanceof RuntimeException) {
            throw RuntimeException.class.cast(attribute);
        } else if (attribute instanceof IOException) {
            throw IOException.class.cast(attribute);
        } else {
            throw new IOException("Dispatch error: " + attribute);
        }
    }

    private static HttpServletRequest wrap(HttpServletRequest delegate) {
        // see AppDispatcher::processRequest
        delegate.setAttribute(ApplicationFilterFactory.DISPATCHER_TYPE_ATTR, ApplicationFilterFactory.REQUEST_INTEGER);
        return new HttpServletRequestWrapper(new RequestFacadeHack(delegate));
    }

    private static Method getInvoke() throws Exception {
        final Class<?> clazz = Hack.class.getClassLoader().loadClass("org.apache.catalina.core.ApplicationDispatcher");
        final Method m = clazz.getDeclaredMethod("invoke", ServletRequest.class, ServletResponse.class);
        m.setAccessible(true);
        return m;
    }

    private static class RequestFacadeHack extends RequestFacade {
        RequestFacadeHack(HttpServletRequest delegate) {
            super(new RequestHack(delegate));
        }
    }

    private static class RequestHack extends Request {
        private HttpServletRequest delegate;

        RequestHack(HttpServletRequest delegate) {
            this.delegate = delegate;
            setFilterChain(new ApplicationFilterChain());
        }

        public HttpServletRequest getRequest() {
            return null;
        }

        public boolean isUserInRole(String s) {
            return delegate.isUserInRole(s);
        }

        public Principal getUserPrincipal() {
            return delegate.getUserPrincipal();
        }

        public String getAuthType() {
            return delegate.getAuthType();
        }

        public int getLocalPort() {
            return delegate.getLocalPort();
        }

        public Cookie[] getCookies() {
            return delegate.getCookies();
        }

        public long getDateHeader(String s) {
            return delegate.getDateHeader(s);
        }

        public String getHeader(String s) {
            return delegate.getHeader(s);
        }

        public Enumeration getHeaders(String s) {
            return delegate.getHeaders(s);
        }

        public Enumeration getHeaderNames() {
            return delegate.getHeaderNames();
        }

        public int getIntHeader(String s) {
            return delegate.getIntHeader(s);
        }

        public String getMethod() {
            return delegate.getMethod();
        }

        public String getPathInfo() {
            return delegate.getPathInfo();
        }

        public String getPathTranslated() {
            return delegate.getPathTranslated();
        }

        public String getContextPath() {
            return delegate.getContextPath();
        }

        public String getQueryString() {
            return delegate.getQueryString();
        }

        public String getRemoteUser() {
            return delegate.getRemoteUser();
        }

        public String getRequestedSessionId() {
            return delegate.getRequestedSessionId();
        }

        public String getRequestURI() {
            return delegate.getRequestURI();
        }

        public StringBuffer getRequestURL() {
            return delegate.getRequestURL();
        }

        public String getServletPath() {
            return delegate.getServletPath();
        }

        public HttpSession getSession(boolean b) {
            return delegate.getSession(b);
        }

        public HttpSession getSession() {
            return delegate.getSession();
        }

        public boolean isRequestedSessionIdValid() {
            return delegate.isRequestedSessionIdValid();
        }

        public boolean isRequestedSessionIdFromCookie() {
            return delegate.isRequestedSessionIdFromCookie();
        }

        public boolean isRequestedSessionIdFromURL() {
            return delegate.isRequestedSessionIdFromURL();
        }

        @SuppressWarnings("deprecation")
        public boolean isRequestedSessionIdFromUrl() {
            return delegate.isRequestedSessionIdFromUrl();
        }

        public Object getAttribute(String s) {
            return delegate.getAttribute(s);
        }

        public Enumeration getAttributeNames() {
            return delegate.getAttributeNames();
        }

        public String getCharacterEncoding() {
            return delegate.getCharacterEncoding();
        }

        public void setCharacterEncoding(String s)
                throws UnsupportedEncodingException {
            delegate.setCharacterEncoding(s);
        }

        public int getContentLength() {
            return delegate.getContentLength();
        }

        public String getContentType() {
            return delegate.getContentType();
        }

        public ServletInputStream getInputStream()
                throws IOException {
            return delegate.getInputStream();
        }

        public String getParameter(String s) {
            return delegate.getParameter(s);
        }

        public Enumeration getParameterNames() {
            return delegate.getParameterNames();
        }

        public String[] getParameterValues(String s) {
            return delegate.getParameterValues(s);
        }

        public Map getParameterMap() {
            return delegate.getParameterMap();
        }

        public String getProtocol() {
            return delegate.getProtocol();
        }

        public String getScheme() {
            return delegate.getScheme();
        }

        public String getServerName() {
            return delegate.getServerName();
        }

        public int getServerPort() {
            return delegate.getServerPort();
        }

        public BufferedReader getReader()
                throws IOException {
            return delegate.getReader();
        }

        public String getRemoteAddr() {
            return delegate.getRemoteAddr();
        }

        public String getRemoteHost() {
            return delegate.getRemoteHost();
        }

        public void setAttribute(String s, Object o) {
            delegate.setAttribute(s, o);
        }

        public void removeAttribute(String s) {
            delegate.removeAttribute(s);
        }

        public Locale getLocale() {
            return delegate.getLocale();
        }

        public Enumeration getLocales() {
            return delegate.getLocales();
        }

        public boolean isSecure() {
            return delegate.isSecure();
        }

        public RequestDispatcher getRequestDispatcher(String s) {
            return delegate.getRequestDispatcher(s);
        }

        @SuppressWarnings("deprecation")
        public String getRealPath(String s) {
            return delegate.getRealPath(s);
        }

        public int getRemotePort() {
            return delegate.getRemotePort();
        }

        public String getLocalName() {
            return delegate.getLocalName();
        }

        public String getLocalAddr() {
            return delegate.getLocalAddr();
        }
    }

    @SuppressWarnings("deprecation")
    static class ResponseHack extends Response {
        private HttpServletResponse delegate;

        public ResponseHack(HttpServletResponse delegate) {
            this.delegate = delegate;
        }

        public void addCookie(Cookie cookie) {
            delegate.addCookie(cookie);
        }

        public boolean containsHeader(String name) {
            return delegate.containsHeader(name);
        }

        public String encodeURL(String url) {
            return delegate.encodeURL(url);
        }

        public String encodeRedirectURL(String url) {
            return delegate.encodeRedirectURL(url);
        }

        public String encodeUrl(String url) {
            return delegate.encodeUrl(url);
        }

        public String encodeRedirectUrl(String url) {
            return delegate.encodeRedirectUrl(url);
        }

        public void sendError(int sc, String msg) throws IOException {
            delegate.sendError(sc, msg);
        }

        public void sendError(int sc) throws IOException {
            delegate.sendError(sc);
        }

        public void sendRedirect(String location) throws IOException {
            delegate.sendRedirect(location);
        }

        public void setDateHeader(String name, long date) {
            delegate.setDateHeader(name, date);
        }

        public void addDateHeader(String name, long date) {
            delegate.addDateHeader(name, date);
        }

        public void setHeader(String name, String value) {
            delegate.setHeader(name, value);
        }

        public void addHeader(String name, String value) {
            delegate.addHeader(name, value);
        }

        public void setIntHeader(String name, int value) {
            delegate.setIntHeader(name, value);
        }

        public void addIntHeader(String name, int value) {
            delegate.addIntHeader(name, value);
        }

        public void setStatus(int sc) {
            delegate.setStatus(sc);
        }

        public void setStatus(int sc, String sm) {
            delegate.setStatus(sc, sm);
        }

        public int getStatus() {
            return delegate.getStatus();
        }

        public String getHeader(String name) {
            return delegate.getHeader(name);
        }

        public Collection<String> getHeaders(String name) {
            return delegate.getHeaders(name);
        }

        public Collection<String> getHeaderNames() {
            return delegate.getHeaderNames();
        }

        public String getCharacterEncoding() {
            return delegate.getCharacterEncoding();
        }

        public String getContentType() {
            return delegate.getContentType();
        }

        public ServletOutputStream getOutputStream() throws IOException {
            return delegate.getOutputStream();
        }

        public PrintWriter getWriter() throws IOException {
            return delegate.getWriter();
        }

        public void setCharacterEncoding(String charset) {
            delegate.setCharacterEncoding(charset);
        }

        public void setContentLength(int len) {
            delegate.setContentLength(len);
        }

        public void setContentType(String type) {
            delegate.setContentType(type);
        }

        public void setBufferSize(int size) {
            delegate.setBufferSize(size);
        }

        public int getBufferSize() {
            return delegate.getBufferSize();
        }

        public void flushBuffer() throws IOException {
            delegate.flushBuffer();
        }

        public void resetBuffer() {
            delegate.resetBuffer();
        }

        public boolean isCommitted() {
            return delegate.isCommitted();
        }

        public void reset() {
            delegate.reset();
        }

        public void setLocale(Locale loc) {
            delegate.setLocale(loc);
        }

        public Locale getLocale() {
            return delegate.getLocale();
        }
    }
}
