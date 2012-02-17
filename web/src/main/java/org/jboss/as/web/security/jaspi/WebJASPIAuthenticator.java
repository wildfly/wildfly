/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.web.security.jaspi;

import org.apache.catalina.Session;
import org.apache.catalina.authenticator.AuthenticatorBase;
import org.apache.catalina.authenticator.Constants;
import org.apache.catalina.connector.Request;
import org.apache.catalina.deploy.LoginConfig;
import org.jboss.as.web.WebLogger;
import org.jboss.as.web.security.JBossGenericPrincipal;
import org.jboss.as.web.security.JBossWebRealm;
import org.jboss.security.SecurityContext;
import org.jboss.security.ServerAuthenticationManager;
import org.jboss.security.SimplePrincipal;
import org.jboss.security.auth.callback.JBossCallbackHandler;
import org.jboss.security.auth.message.GenericMessageInfo;
import org.jboss.security.plugins.auth.JASPIServerAuthenticationManager;

import javax.security.auth.Subject;
import javax.security.auth.message.callback.CallerPrincipalCallback;
import javax.security.auth.message.callback.PasswordValidationCallback;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.security.acl.Group;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;

/**
 * <p>
 * Tomcat authenticator that does JSR-196 (JASPI) authentication.
 * </p>
 *
 * @author <a href="mailto:Anil.Saldhana@redhat.com">Anil Saldhana</a>
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
@SuppressWarnings("unused")
public class WebJASPIAuthenticator extends AuthenticatorBase {

    protected final String messageLayer = "HttpServlet";

    protected String serverAuthenticationManagerClass;

    protected boolean secureResponse;

    @Override
    protected boolean authenticate(Request request, HttpServletResponse response, LoginConfig config) throws IOException {
        boolean result = false;

        String authMethod = config.getAuthMethod();

        // have we already authenticated someone?
        Principal principal = request.getUserPrincipal();
        String ssoId = (String) request.getNote(Constants.REQ_SSOID_NOTE);
        if (principal != null) {
            WebLogger.WEB_SECURITY_LOGGER.tracef("Already authenticated '%s'", principal.getName());
            // associate the session with any existing SSO session
            if (ssoId != null)
                associate(ssoId, request.getSessionInternal(true));
            return (true);
        }

        if ("BASIC".equalsIgnoreCase(authMethod) || "FORM".equalsIgnoreCase(authMethod)) {
            // is there an SSO session against which we can try to reauthenticate?
            if (ssoId != null) {
                WebLogger.WEB_SECURITY_LOGGER.tracef("SSO Id %s set; attempting reauthentication", ssoId);
                /* Try to reauthenticate using data cached by SSO.  If this fails, either the original SSO logon was of
                   DIGEST or SSL (which we can't reauthenticate ourselves because there is no cached username and password),
                   or the realm denied the user's reauthentication for some reason. In either case we have to prompt the
                   user for a logon */
                if (reauthenticateFromSSO(ssoId, request))
                    return true;
            }
        }

        // TODO: change message info to operate on HttpServletResponse, to align with Servlet 3.0
        GenericMessageInfo messageInfo = new GenericMessageInfo();
        messageInfo.setRequestMessage(request);
        messageInfo.setResponseMessage(request.getResponse());

        // put bits of information needed by tomcat server auth modules
        messageInfo.getMap().put("CACHE", String.valueOf(cache));
        messageInfo.getMap().put("javax.security.auth.message.MessagePolicy.isMandatory", "true");

        WebJASPICallbackHandler cbh = new WebJASPICallbackHandler();
        ServerAuthenticationManager sam = getServerAuthenticationManager();
        String appContext = request.getLocalName() + " " + request.getContextPath();
        Subject clientSubject = new Subject();
        if (sam != null) {
            result = sam.isValid(messageInfo, clientSubject, messageLayer, appContext, cbh);
        }

        // the authentication process has been a success. We need to register the principal, username, password and roles
        // with the container
        if (result) {
            PasswordValidationCallback pvc = cbh.getPasswordValidationCallback();
            CallerPrincipalCallback cpc = cbh.getCallerPrincipalCallback();

            // get the client principal from the callback.
            Principal clientPrincipal = cpc.getPrincipal();
            if (clientPrincipal == null) {
                clientPrincipal = new SimplePrincipal(cpc.getName());
            }

            // if the client principal is not a jboss generic principal, we need to build one before registering.
            if (!(clientPrincipal instanceof JBossGenericPrincipal))
                clientPrincipal = this.buildJBossPrincipal(clientSubject, clientPrincipal);

            this.register(request, response, clientPrincipal, authMethod, pvc.getUsername(),
                    new String(pvc.getPassword()));

            if (this.secureResponse)
                sam.secureResponse(messageInfo, new Subject(), messageLayer, appContext, cbh);
        }

        return result;
    }

    /**
     * <p>
     * Obtains the fully-qualified name of the class that implements the {@code ServerAuthenticationManager} interface.
     * </p>
     *
     * @return the name of the {@code ServerAuthenticationManager} implementation class.
     */
    public String getServerAuthenticationManagerClass() {
        return serverAuthenticationManagerClass;
    }

    /**
     * <p>
     * Sets the fully-qualified name of the class that implements the {@code ServerAuthenticationManager} interface.
     * </p>
     *
     * @param serverAuthenticationManagerClass
     *         the name {@code ServerAuthenticationManager} implementation class.
     */
    public void setServerAuthenticationManagerClass(String serverAuthenticationManagerClass) {
        this.serverAuthenticationManagerClass = serverAuthenticationManagerClass;
    }

    /**
     * <p>
     * Obtains the value of the flag that indicates whether JASPI should be used to secure the HTTP response or not.
     * </p>
     *
     * @return {@code true} if the authenticator is to secure the response; {@code false} otherwise.
     */
    public boolean getSecureResponse() {
        return this.secureResponse;
    }

    /**
     * <p>
     * Sets the flag that indicates whether JASPI should be used to secure the HTTP response or not.
     * </p>
     *
     * @param secureResponse {@code true} if the authenticator is to secure the response; {@code false} otherwise.
     */
    public void setSecureResponse(boolean secureResponse) {
        this.secureResponse = secureResponse;
    }

    /**
     * <p>
     * Instantiates and returns the {@code ServerAuthenticationManager} implementation.
     * </p>
     *
     * @return a reference to the instantiated {@code ServerAuthenticationManager} instance.
     */
    protected ServerAuthenticationManager getServerAuthenticationManager() {
        ServerAuthenticationManager sam = null;
        // if the default authentication manager is to be used, just instantiate it.
        if (this.serverAuthenticationManagerClass ==  null) {
            SecurityContext context = SecurityActions.getSecurityContext();
            if (context != null) {
                WebLogger.WEB_SECURITY_LOGGER.debugf("Instantiating JASPI authentication manager with security domain %s",
                        context.getSecurityDomain());
                sam = new JASPIServerAuthenticationManager(context.getSecurityDomain(), new JBossCallbackHandler());
            }
            else {
                WebLogger.WEB_SECURITY_LOGGER.debugf("Security context is null, instantiating JASPI authentication manager with default domain");
                sam = new JASPIServerAuthenticationManager();
            }
        }
        // if not, load the configured authentication manager via reflection.
        else {
            Class<?> clazz;
            try {
                clazz = SecurityActions.loadClass(this.serverAuthenticationManagerClass);
                sam = (ServerAuthenticationManager) clazz.newInstance();
            } catch (Exception e) {
                WebLogger.WEB_SECURITY_LOGGER.noServerAuthenticationManager(e);
            }
        }
        return sam;
    }

    /**
     * <p>
     * Registers an authenticated Principal and authentication type in our request, in the current session (if there is one),
     * and with our SingleSignOn valve, if there is one.  Set the appropriate cookie to be returned.
     * </p>
     *
     * @param request   the servlet request we are processing.
     * @param response  the servlet response we are generating.
     * @param principal the authenticated {@code Principal} to be registered.
     * @param authType  the authentication type to be registered.
     * @param username  username used to authenticate (if any).
     * @param password  password used to authenticate (if any).
     */
    protected void register(Request request, HttpServletResponse response, Principal principal, String authType,
                            String username, String password) {

        if (WebLogger.WEB_SECURITY_LOGGER.isTraceEnabled()) {
            // Bugzilla 39255: http://issues.apache.org/bugzilla/show_bug.cgi?id=39255
            String name = (principal == null) ? "none" : principal.getName();
            WebLogger.WEB_SECURITY_LOGGER.tracef("Authenticated '%s' with type '" + authType + "'", name, authType);
        }

        // cache the authentication information in our request
        request.setAuthType(authType);
        request.setUserPrincipal(principal);

        Session session = request.getSessionInternal(false);
        // cache the authentication information in our session, if any
        if (cache) {
            if (session != null) {
                session.setAuthType(authType);
                session.setPrincipal(principal);
                if (username != null)
                    session.setNote(Constants.SESS_USERNAME_NOTE, username);
                else
                    session.removeNote(Constants.SESS_USERNAME_NOTE);
                if (password != null)
                    session.setNote(Constants.SESS_PASSWORD_NOTE, password);
                else
                    session.removeNote(Constants.SESS_PASSWORD_NOTE);
            }
        }

        // construct a cookie to be returned to the client
        if (sso == null)
            return;

        // only create a new SSO entry if the SSO did not already set a note for an existing entry (as it would do with
        // subsequent requests for DIGEST and SSL authenticated contexts).
        String ssoId = (String) request.getNote(Constants.REQ_SSOID_NOTE);
        if (ssoId == null) {
            // construct a cookie to be returned to the client.
            ssoId = generateSessionId(new Random());
            Cookie cookie = new Cookie(Constants.SINGLE_SIGN_ON_COOKIE, ssoId);
            cookie.setMaxAge(-1);
            cookie.setPath("/");

            // Bugzilla 41217
            cookie.setSecure(request.isSecure());

            // Bugzilla 34724
            String ssoDomain = sso.getCookieDomain();
            if (ssoDomain != null) {
                cookie.setDomain(ssoDomain);
            }

            response.addCookie(cookie);

            // register this principal with our SSO valve.
            sso.register(ssoId, principal, authType, username, password);
            request.setNote(Constants.REQ_SSOID_NOTE, ssoId);

        } else {
            // update the SSO session with the latest authentication data.
            sso.update(ssoId, principal, authType, username, password);
        }

        // fix for Bug 10040: always associate a session with a new SSO registration. SSO entries are only removed from
        // the SSO registry map when associated sessions are destroyed; if a new SSO entry is created above for this
        // request and the user never revisits the context, the SSO entry will never be cleared if we don't associate
        // the session
        if (session == null)
            session = request.getSessionInternal(true);
        sso.associate(ssoId, session);
    }

    protected Principal buildJBossPrincipal(Subject subject, Principal principal) {

        List<String> roles = new ArrayList<String>();
        // look for roles in the subject first.
        for (Principal p : subject.getPrincipals()) {
            if (p instanceof Group && p.getName().equals("Roles")) {
                Enumeration<? extends Principal> members = ((Group) p).members();
                while (members.hasMoreElements())
                    roles.add(members.nextElement().getName());
            }
        }

        // if the subject didn't contain any roles, look for the roles declared in the deployment descriptor.
        JBossWebRealm realm = (JBossWebRealm) this.getContainer().getRealm();
        if (roles.isEmpty())
            roles.addAll(realm.getPrincipalVersusRolesMap().get(principal.getName()));

        // build and return the JBossGenericPrincipal.
        return new JBossGenericPrincipal(realm, principal.getName(), null, roles, principal, null, null, null, subject);

    }

}