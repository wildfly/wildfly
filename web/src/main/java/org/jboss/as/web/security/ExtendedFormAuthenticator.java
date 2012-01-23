/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.catalina.Session;
import org.apache.catalina.authenticator.Constants;
import org.apache.catalina.authenticator.FormAuthenticator;
import org.apache.catalina.connector.Request;
import org.apache.catalina.deploy.LoginConfig;
import org.jboss.as.web.WebLogger;

/**
 * An extension of the form authenticator that associates the j_username with the session under the attribute name j_username
 * for use by form login/error pages. If the includePassword attribute is true, the j_password value is also included in the
 * session under the attribute name j_password. In addition, it maps any authentication exception found in the
 * SecurityAssociation to the session attribute name j_exception.
 *
 * @author Scott.Stark@jboss.org
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public class ExtendedFormAuthenticator extends FormAuthenticator {

    public static final String LOGIN_EXCEPTION = "j_exception";

    public static final String DID_POPULATE = "did_populate";

    private boolean includePassword;

    public boolean isIncludePassword() {
        return includePassword;
    }

    public void setIncludePassword(boolean includePassword) {
        this.includePassword = includePassword;
    }

    /**
     * Authenticate the user making this request, based on the specified login configuration. Return <code>true</code> if any
     * specified constraint has been satisfied, or <code>false</code> if we have created a response challenge already.
     *
     * @param request Request we are processing
     * @param response Response we are creating
     * @param config Login configuration describing how authentication should be performed
     *
     * @exception IOException if an input/output error occurs
     */
    @Override
    public boolean authenticate(Request request, HttpServletResponse response, LoginConfig config) throws IOException {

        boolean didPopulate = false;

        // let super class handle the authenticate().
        boolean alreadyAuthenticated = super.authenticate(request, response, config);

        Session session = request.getSessionInternal(false);
        if (session != null) {
            // get session note(used internally) to indicate if did populateSession.
            Boolean b = (Boolean) session.getNote(DID_POPULATE);
            if (b != null)
                didPopulate = b.booleanValue();
        }

        // if user not already authenticated and did populate not called..
        if (!alreadyAuthenticated && !didPopulate) {
            populateSession(request);
        }

        // remove the note since not needed anymore, if set.
        if (session != null)
            session.removeNote(DID_POPULATE);

        // pass return value on.
        return alreadyAuthenticated;
    }

    /**
     * Dispatch to the form error-page
     *
     * @param request Request we are processing
     * @param response Response we are creating
     * @param config Login configuration describing how authentication should be performed
     */
    @Override
    protected void forwardToErrorPage(Request request, HttpServletResponse response, LoginConfig config) throws IOException {
        WebLogger.WEB_SECURITY_LOGGER.tracef("forwardToErrorPage");
        populateSession(request);
        super.forwardToErrorPage(request, response, config);
        SecurityActions.clearAuthException();
    }

    /**
     * Dispatch to the form login-page
     *
     * @param request Request we are processing
     * @param response Response we are creating
     * @param config Login configuration describing how authentication should be performed
     */
    @Override
    protected void forwardToLoginPage(Request request, HttpServletResponse response, LoginConfig config) throws IOException {
        WebLogger.WEB_SECURITY_LOGGER.tracef("forwardToLoginPage");
        populateSession(request);
        super.forwardToLoginPage(request, response, config);
    }

    /**
     * Populates the session the request belongs to with authentication data as described above. If the request does not have an
     * associated session does nothing.
     *
     * @param request Request we are processing
     */
    protected void populateSession(Request request) {
        Session session = request.getSessionInternal(false);

        // if there is a session to store data under...
        if (session != null) {
            HttpSession httpSession = session.getSession();

            WebLogger.WEB_SECURITY_LOGGER.tracef("SessionID: " + httpSession.getId());

            // store username.
            String username = request.getParameter(Constants.FORM_USERNAME);
            WebLogger.WEB_SECURITY_LOGGER.tracef("Setting " + Constants.FORM_USERNAME + " = " + username);
            httpSession.setAttribute(Constants.FORM_USERNAME, username);

            // store password if requested.
            if (includePassword) {
                String password = request.getParameter(Constants.FORM_PASSWORD);
                String displayPassword = (password == null ? " = null" : " = --hidden--");
                WebLogger.WEB_SECURITY_LOGGER.tracef("Setting " + Constants.FORM_PASSWORD + displayPassword);
                httpSession.setAttribute(Constants.FORM_PASSWORD, password);
            }

            // store SecurityAssociation context exception.
            Throwable t = SecurityActions.getAuthException();
            WebLogger.WEB_SECURITY_LOGGER.tracef("Setting " + LOGIN_EXCEPTION + " = " + t);
            httpSession.setAttribute(LOGIN_EXCEPTION, t);

            // finally, set a note so we do not do this again.
            session.setNote(DID_POPULATE, Boolean.TRUE);
        } else {
            WebLogger.WEB_SECURITY_LOGGER.tracef("No Session to store login parameters in");
        }
    }

}
