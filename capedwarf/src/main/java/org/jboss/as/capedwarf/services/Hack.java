package org.jboss.as.capedwarf.services;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.RequestFacade;
import org.apache.catalina.core.ApplicationFilterChain;
import org.apache.catalina.core.ApplicationFilterFactory;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

/**
 * Hack around to get StandardContext, Wrapper, etc.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
class Hack {

    static HttpServletRequest wrap(HttpServletRequest delegate) {
        // see AppDispatcher::processRequest
        delegate.setAttribute(ApplicationFilterFactory.DISPATCHER_TYPE_ATTR, ApplicationFilterFactory.REQUEST_INTEGER);
        return new HttpServletRequestWrapper(new RequestFacadeHack(delegate));
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
}
