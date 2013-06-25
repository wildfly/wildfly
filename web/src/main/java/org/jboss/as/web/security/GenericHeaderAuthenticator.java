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

package org.jboss.as.web.security;

import java.io.IOException;
import java.security.Principal;
import java.util.StringTokenizer;

import javax.management.JMException;
import javax.management.ObjectName;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Realm;
import org.apache.catalina.Session;
import org.apache.catalina.authenticator.Constants;
import org.apache.catalina.connector.Request;
import org.apache.catalina.deploy.LoginConfig;
import org.jboss.logging.Logger;

/**
 * JBAS-2283: Provide custom header based authentication support
 * Header Authenticator that deals with userid from the request header Requires two attributes configured on the Tomcat Service
 * - one for the http header denoting the authenticated identity and the other is the SESSION cookie
 * @author <a href="mailto:Anil.Saldhana@jboss.org">Anil Saldhana</a>
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 * @version $Revision$
 * @since Sep 11, 2006
 */
public class GenericHeaderAuthenticator extends ExtendedFormAuthenticator {
    protected static Logger log = Logger.getLogger(GenericHeaderAuthenticator.class);

    protected boolean trace = log.isTraceEnabled();

    // JBAS-4804: GenericHeaderAuthenticator injection of ssoid and
    // sessioncookie name.
    private String httpHeaderForSSOAuth = null;

    private String sessionCookieForSSOAuth = null;

    /**
     * <p>
     * Obtain the value of the <code>httpHeaderForSSOAuth</code> attribute. This attribute is used to indicate the request
     * header ids that have to be checked in order to retrieve the SSO identity set by a third party security system.
     * </p>
     * @return a <code>String</code> containing the value of the <code>httpHeaderForSSOAuth</code> attribute.
     */
    public String getHttpHeaderForSSOAuth() {
        return httpHeaderForSSOAuth;
    }

    /**
     * <p>
     * Set the value of the <code>httpHeaderForSSOAuth</code> attribute. This attribute is used to indicate the request header
     * ids that have to be checked in order to retrieve the SSO identity set by a third party security system.
     * </p>
     * @param httpHeaderForSSOAuth a <code>String</code> containing the value of the <code>httpHeaderForSSOAuth</code>
     *        attribute.
     */
    public void setHttpHeaderForSSOAuth(String httpHeaderForSSOAuth) {
        this.httpHeaderForSSOAuth = httpHeaderForSSOAuth;
    }

    /**
     * <p>
     * Obtain the value of the <code>sessionCookieForSSOAuth</code> attribute. This attribute is used to indicate the names of
     * the SSO cookies that may be present in the request object.
     * </p>
     * @return a <code>String</code> containing the names (separated by a <code>','</code>) of the SSO cookies that may have
     *         been set by a third party security system in the request.
     */
    public String getSessionCookieForSSOAuth() {
        return sessionCookieForSSOAuth;
    }

    /**
     * <p>
     * Set the value of the <code>sessionCookieForSSOAuth</code> attribute. This attribute is used to indicate the names of the
     * SSO cookies that may be present in the request object.
     * </p>
     * @param sessionCookieForSSOAuth a <code>String</code> containing the names (separated by a <code>','</code>) of the SSO
     *        cookies that may have been set by a third party security system in the request.
     */
    public void setSessionCookieForSSOAuth(String sessionCookieForSSOAuth) {
        this.sessionCookieForSSOAuth = sessionCookieForSSOAuth;
    }

    /**
     * <p>
     * Creates an instance of <code>GenericHeaderAuthenticator</code>.
     * </p>
     */
    public GenericHeaderAuthenticator() {
        super();
    }

    public boolean authenticate(Request request, HttpServletResponse response, LoginConfig config) throws IOException {
        log.trace("Authenticating user");

        Principal principal = request.getUserPrincipal();
        if (principal != null) {
            if (trace)
                log.trace("Already authenticated '" + principal.getName() + "'");
            return true;
        }

        Realm realm = context.getRealm();
        Session session = request.getSessionInternal(true);

        String username = getUserId(request);
        String password = getSessionCookie(request);

        // Check if there is sso id as well as sessionkey
        if (username == null || password == null) {
            log.trace("Username is null or password(sessionkey) is null:fallback to form auth");
            return super.authenticate(request, response, config);
        }
        principal = realm.authenticate(username, password);

        if (principal == null) {
            forwardToErrorPage(request, response, config);
            return false;
        }

        session.setNote(Constants.SESS_USERNAME_NOTE, username);
        session.setNote(Constants.SESS_PASSWORD_NOTE, password);
        request.setUserPrincipal(principal);

        register(request, response, principal, HttpServletRequest.FORM_AUTH, username, password);
        return true;
    }

    /**
     * Get the username from the request header
     * @param request
     * @return
     */
    protected String getUserId(Request request) {
        String ssoid = null;
        // We can have a comma-separated ids
        String ids = "";
        try {
            ids = this.getIdentityHeaderId();
        } catch (JMException e) {
            if (trace)
                log.trace("getUserId exception", e);
        }
        if (ids == null || ids.length() == 0)
            throw new IllegalStateException("Http headers configuration in tomcat service missing");

        StringTokenizer st = new StringTokenizer(ids, ",");
        while (st.hasMoreTokens()) {
            ssoid = request.getHeader(st.nextToken());
            if (ssoid != null)
                break;
        }
        if (trace)
            log.trace("SSOID-" + ssoid);
        return ssoid;
    }

    /**
     * Obtain the session cookie from the request
     * @param request
     * @return
     */
    protected String getSessionCookie(Request request) {
        Cookie[] cookies = request.getCookies();
        int numCookies = cookies != null ? cookies.length : 0;

        // We can have comma-separated ids
        String ids = "";
        try {
            ids = this.getSessionCookieId();
            log.trace("Session Cookie Ids=" + ids);
        } catch (JMException e) {
            if (trace)
                log.trace("checkSessionCookie exception", e);
        }
        if (ids == null || ids.length() == 0)
            throw new IllegalStateException("Session cookies configuration in tomcat service missing");

        StringTokenizer st = new StringTokenizer(ids, ",");
        while (st.hasMoreTokens()) {
            String cookieToken = st.nextToken();
            String val = getCookieValue(cookies, numCookies, cookieToken);
            if (val != null)
                return val;
        }
        if (trace)
            log.trace("Session Cookie not found");
        return null;
    }

    /**
     * Get the configured header identity id in the tomcat service
     * @return
     * @throws JMException
     */
    protected String getIdentityHeaderId() throws JMException {
        if (this.httpHeaderForSSOAuth != null)
            return this.httpHeaderForSSOAuth;
        return (String) mserver.getAttribute(new ObjectName("jboss.web:service=WebServer"), "HttpHeaderForSSOAuth");
    }

    /**
     * Get the configured session cookie id in the tomcat service
     * @return
     * @throws JMException
     */
    protected String getSessionCookieId() throws JMException {
        if (this.sessionCookieForSSOAuth != null)
            return this.sessionCookieForSSOAuth;
        return (String) mserver.getAttribute(new ObjectName("jboss.web:service=WebServer"), "SessionCookieForSSOAuth");
    }

    /**
     * Get the value of a cookie if the name matches the token
     * @param cookies array of cookies
     * @param numCookies number of cookies in the array
     * @param token Key
     * @return value of cookie
     */
    protected String getCookieValue(Cookie[] cookies, int numCookies, String token) {
        for (int i = 0; i < numCookies; i++) {
            Cookie cookie = cookies[i];
            log.trace("Matching cookieToken:" + token + " with cookie name=" + cookie.getName());
            if (token.equals(cookie.getName())) {
                if (trace)
                    log.trace("Cookie-" + token + " value=" + cookie.getValue());
                return cookie.getValue();
            }
        }
        return null;
    }
}
