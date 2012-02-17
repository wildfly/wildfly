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

package org.jboss.as.web.security.jaspi.modules;

import org.apache.catalina.Context;
import org.apache.catalina.Realm;
import org.apache.catalina.Session;
import org.apache.catalina.authenticator.Constants;
import org.apache.catalina.authenticator.SavedRequest;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.util.StringManager;
import org.apache.coyote.ActionCode;
import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.tomcat.util.buf.CharChunk;
import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.http.MimeHeaders;
import org.jboss.as.web.WebLogger;

import javax.security.auth.Subject;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;

/**
 * <p>
 * This class implements a JASPI {@code ServerAuthModule} that handles HTTP FORM authentication.
 * </p>
 *
 * @author <a href="mailto:Anil.Saldhana@redhat.com">Anil Saldhana</a>
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
@SuppressWarnings("unused")
public class HTTPFormServerAuthModule extends WebServerAuthModule {

    protected Context context;

    protected boolean cache = false;

    protected static final StringManager sm = StringManager.getManager(Constants.Package);

    protected String delegatingLoginContextName = null;

    /**
     * <p>
     * Creates an instance of {@code HTTPFormServerAuthModule}.
     * </p>
     */
    public HTTPFormServerAuthModule() {
    }

    /**
     * <p>
     * Creates an instance of {@code HTTPFormServerAuthModule} with the specified delegating login context name.
     * </p>
     *
     * @param delegatingLoginContextName the name of the login context configuration that contains the JAAS modules that
     *                                   are to be called by this module.
     */
    public HTTPFormServerAuthModule(String delegatingLoginContextName) {
        super();
        this.delegatingLoginContextName = delegatingLoginContextName;
    }

    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject)
            throws AuthException {
        // Do nothing, just return SUCCESS.
        return AuthStatus.SUCCESS;
    }

    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject)
            throws AuthException {

        Request request = (Request) messageInfo.getRequestMessage();
        Response response = (Response) messageInfo.getResponseMessage();

        Principal principal;
        context = request.getContext();
        LoginConfig config = context.getLoginConfig();

        // references to objects we will need later.
        Session session = null;

        // lets find out if the cache is enabled or not.
        cache = Boolean.valueOf((String) messageInfo.getMap().get("CACHE"));

        // have we authenticated this user before but have caching disabled?
        if (!cache) {
            session = request.getSessionInternal(true);
            WebLogger.WEB_SECURITY_LOGGER.debugf("Checking for reauthenticate in session %s", session.getIdInternal());
            String username = (String) session.getNote(Constants.SESS_USERNAME_NOTE);
            String password = (String) session.getNote(Constants.SESS_PASSWORD_NOTE);
            if ((username != null) && (password != null)) {
                WebLogger.WEB_SECURITY_LOGGER.debugf("Reauthenticating username '%s'", username);
                principal = context.getRealm().authenticate(username, password);
                if (principal != null) {
                    session.setNote(Constants.FORM_PRINCIPAL_NOTE, principal);
                    if (!matchRequest(request)) {
                        registerWithCallbackHandler(principal, username, password);
                        return AuthStatus.SUCCESS;
                    }
                }
                WebLogger.WEB_SECURITY_LOGGER.tracef("Reauthentication failed, proceed normally");
            }
        }

        // is this the re-submit of the original request URI after successful authentication?  If so, forward the *original* request instead.
        if (matchRequest(request)) {
            session = request.getSessionInternal(true);
            WebLogger.WEB_SECURITY_LOGGER.tracef("Restore request from session '%s'", session.getIdInternal());
            principal = (Principal) session.getNote(Constants.FORM_PRINCIPAL_NOTE);

            registerWithCallbackHandler(principal,
                    (String) session.getNote(Constants.SESS_USERNAME_NOTE),
                    (String) session.getNote(Constants.SESS_PASSWORD_NOTE));

            // if we're caching principals we no longer need the username and password in the session, so remove them.
            if (cache) {
                session.removeNote(Constants.SESS_USERNAME_NOTE);
                session.removeNote(Constants.SESS_PASSWORD_NOTE);
            }
            if (restoreRequest(request, session)) {
                WebLogger.WEB_SECURITY_LOGGER.tracef("Proceed to restored request");
                return (AuthStatus.SUCCESS);
            } else {
                WebLogger.WEB_SECURITY_LOGGER.tracef("Restore of original request failed");

                try {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                } catch (IOException e) {
                 // Ignore IOException here (client disconnect)
                }
                return AuthStatus.FAILURE;
            }
        }

        // acquire references to objects we will need to evaluate.
        MessageBytes uriMB = MessageBytes.newInstance();
        CharChunk uriCC = uriMB.getCharChunk();
        uriCC.setLimit(-1);
        String contextPath = request.getContextPath();
        String requestURI = request.getDecodedRequestURI();

        // is this the action request from the login page?
        boolean loginAction = requestURI.startsWith(contextPath) && requestURI.endsWith(Constants.FORM_ACTION);

        // no - save this request and redirect to the form login page.
        if (!loginAction) {
            session = request.getSessionInternal(true);
            WebLogger.WEB_SECURITY_LOGGER.tracef("Save request in session '%s'", session.getIdInternal());
            try {
                saveRequest(request, session);
            } catch (IOException ioe) {
                WebLogger.WEB_SECURITY_LOGGER.tracef("Request body too big to save during authentication");
                try {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, sm.getString("authenticator.requestBodyTooBig"));
                } catch (IOException e) {
                 // Ignore IOException here (client disconnect)
                    throw new AuthException(e.getLocalizedMessage());
                }
                return (AuthStatus.FAILURE);
            }
            forwardToLoginPage(request, response, config);
            return (AuthStatus.SEND_CONTINUE);
        }

        // yes - validate the specified credentials and redirect to the error page if they are not correct
        Realm realm = context.getRealm();
        String characterEncoding = request.getCharacterEncoding();
        if (characterEncoding != null) {
            try {
                request.setCharacterEncoding(characterEncoding);
            } catch (UnsupportedEncodingException e) {
                WebLogger.WEB_SECURITY_LOGGER.unsupportedEncoding(e.getLocalizedMessage());
            }
        }
        String username = request.getParameter(Constants.FORM_USERNAME);
        String password = request.getParameter(Constants.FORM_PASSWORD);

        WebLogger.WEB_SECURITY_LOGGER.tracef("Authenticating username '%s'", username);
        principal = realm.authenticate(username, password);
        if (principal == null) {
            forwardToErrorPage(request, response, config);
            return (AuthStatus.FAILURE);
        }

        WebLogger.WEB_SECURITY_LOGGER.tracef("Authentication of '%s' was successful", username);
        if (session == null)
            session = request.getSessionInternal(false);
        if (session == null) {
            WebLogger.WEB_SECURITY_LOGGER.tracef("User took so long to log on the session expired");
            try {
                response.sendError(HttpServletResponse.SC_REQUEST_TIMEOUT, sm.getString("authenticator.sessionExpired"));
            } catch (IOException e) {
             // Ignore IOException here (client disconnect)
            }
            return (AuthStatus.FAILURE);
        }

        // save the authenticated Principal in our session.
        session.setNote(Constants.FORM_PRINCIPAL_NOTE, principal);

        // save the username and password as well.
        session.setNote(Constants.SESS_USERNAME_NOTE, username);
        session.setNote(Constants.SESS_PASSWORD_NOTE, password);

        // redirect the user to the original request URI (which will cause the original request to be restored).
        requestURI = savedRequestURL(session);
        WebLogger.WEB_SECURITY_LOGGER.tracef("Redirecting to original '%s'", requestURI);
        try {
            if (requestURI == null)
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, sm.getString("authenticator.formlogin"));
            else
                response.sendRedirect(response.encodeRedirectURL(requestURI));
        } catch (IOException ioe) {
         // Ignore IOException here (client disconnect)
        }
        return (AuthStatus.FAILURE);
    }

    /**
     * <p>
     * This method verifies if the specified {@code Request} matches a previously saved request or not.
     * </p>
     *
     * @param request The {@code Request} object to be verified.
     * @return {@code true} if the request matches the saved one; {@code false} otherwise.
     */
    protected boolean matchRequest(Request request) {
        // has a session been created?
        Session session = request.getSessionInternal(false);
        if (session == null)
            return (false);

        // is there a saved request?
        SavedRequest sreq = (SavedRequest) session.getNote(Constants.FORM_REQUEST_NOTE);
        if (sreq == null)
            return (false);

        // is there a saved principal?
        if (session.getNote(Constants.FORM_PRINCIPAL_NOTE) == null)
            return (false);

        // does the request URI match?
        String requestURI = request.getRequestURI();
        if (requestURI == null)
            return (false);
        return (requestURI.equals(sreq.getRequestURI()));

    }


    /**
     * <p>
     * Restores the original request from information stored in our session. If the original request is no longer present
     * (because the session timed out), return {@code false}; otherwise, return {@code true}.
     * </p>
     *
     * @param request the {@code Request} instance that will be populated with the saved request data.
     * @param session the {@code Session} containing the saved information.
     * @return {@code true} if the request could be restored; {@code false} otherwise.
     */
    @SuppressWarnings("unchecked")
    protected boolean restoreRequest(Request request, Session session) {

        // retrieve and remove the SavedRequest object from our session.
        SavedRequest saved = (SavedRequest) session.getNote(Constants.FORM_REQUEST_NOTE);
        session.removeNote(Constants.FORM_REQUEST_NOTE);
        session.removeNote(Constants.FORM_PRINCIPAL_NOTE);
        if (saved == null)
            return (false);

        // modify our current request to reflect the original one
        request.clearCookies();
        Iterator cookies = saved.getCookies();
        while (cookies.hasNext()) {
            request.addCookie((Cookie) cookies.next());
        }

        MimeHeaders rmh = request.getCoyoteRequest().getMimeHeaders();
        rmh.recycle();
        boolean cachable = "GET".equalsIgnoreCase(saved.getMethod()) || "HEAD".equalsIgnoreCase(saved.getMethod());
        Iterator names = saved.getHeaderNames();
        while (names.hasNext()) {
            String name = (String) names.next();
            // the browser isn't expecting this conditional response now. Assuming that it can quietly recover from an unexpected 412.
            if (!("If-Modified-Since".equalsIgnoreCase(name) || (cachable && "If-None-Match".equalsIgnoreCase(name)))) {
                Iterator values = saved.getHeaderValues(name);
                while (values.hasNext()) {
                    rmh.addValue(name).setString((String) values.next());
                }
            }
        }

        request.clearLocales();
        Iterator locales = saved.getLocales();
        while (locales.hasNext()) {
            request.addLocale((Locale) locales.next());
        }

        request.getCoyoteRequest().getParameters().recycle();

        if ("POST".equalsIgnoreCase(saved.getMethod())) {
            ByteChunk body = saved.getBody();

            if (body != null) {
                request.getCoyoteRequest().action(ActionCode.ACTION_REQ_SET_BODY_REPLAY, body);

                // set the MIME content type.
                MessageBytes contentType = MessageBytes.newInstance();

                // if no content type specified, use default for POST.
                String savedContentType = saved.getContentType();
                if (savedContentType == null) {
                    savedContentType = "application/x-www-form-urlencoded";
                }

                contentType.setString(savedContentType);
                request.getCoyoteRequest().setContentType(contentType);
            }
        }
        request.getCoyoteRequest().method().setString(saved.getMethod());
        request.getCoyoteRequest().queryString().setString(saved.getQueryString());
        request.getCoyoteRequest().requestURI().setString(saved.getRequestURI());

        return true;
    }


    /**
     * <p>
     * Saves the original request information into our session.
     * </p>
     *
     * @param request the {@code Request} instance to be saved
     * @param session the {@code Session} where the request is to be saved.
     * @throws IOException if an error occurs while saving the request.
     */
    @SuppressWarnings("unchecked")
    protected void saveRequest(Request request, Session session) throws IOException {

        // create and populate a SavedRequest object for this request.
        SavedRequest saved = new SavedRequest();
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies)
                saved.addCookie(cookie);
        }
        Enumeration names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            Enumeration values = request.getHeaders(name);
            while (values.hasMoreElements()) {
                String value = (String) values.nextElement();
                saved.addHeader(name, value);
            }
        }
        Enumeration locales = request.getLocales();
        while (locales.hasMoreElements()) {
            Locale locale = (Locale) locales.nextElement();
            saved.addLocale(locale);
        }

        if ("POST".equalsIgnoreCase(request.getMethod())) {
            ByteChunk body = new ByteChunk();
            body.setLimit(request.getConnector().getMaxSavePostSize());

            byte[] buffer = new byte[4096];
            int bytesRead;
            InputStream is = request.getInputStream();

            while ((bytesRead = is.read(buffer)) >= 0) {
                body.append(buffer, 0, bytesRead);
            }
            saved.setBody(body);
            saved.setContentType(request.getContentType());
        }

        saved.setMethod(request.getMethod());
        saved.setQueryString(request.getQueryString());
        saved.setRequestURI(request.getRequestURI());

        // stash the SavedRequest in our session for later use.
        session.setNote(Constants.FORM_REQUEST_NOTE, saved);
    }

    /**
     * <p>
     * Returns the request URI (with the corresponding query string, if any) from the saved request so that we can
     * redirect to it.
     * </p>
     *
     * @param session the current {@code Session}.
     * @return a {@code String} containing the request URI.
     */
    protected String savedRequestURL(Session session) {

        SavedRequest saved = (SavedRequest) session.getNote(Constants.FORM_REQUEST_NOTE);
        if (saved == null)
            return (null);
        StringBuilder sb = new StringBuilder(saved.getRequestURI());
        if (saved.getQueryString() != null) {
            sb.append('?');
            sb.append(saved.getQueryString());
        }
        return (sb.toString());

    }

    /**
     * <p>
     * Forwards the current request to the login page.
     * </p>
     *
     * @param request  the {@code Request} instance being processed.
     * @param response the {@code Response} instance being processed.
     * @param config   the login configuration describing how authentication should be performed
     */
    protected void forwardToLoginPage(Request request, Response response, LoginConfig config) {
        RequestDispatcher disp = this.context.getServletContext().getRequestDispatcher(config.getLoginPage());
        try {
            disp.forward(request.getRequest(), response.getResponse());
            response.finishResponse();
        } catch (Throwable t) {
            WebLogger.WEB_SECURITY_LOGGER.errorForwardingToLoginPage(t.getLocalizedMessage());
        }
    }


    /**
     * <p>
     * Forwards the current request to the error page.
     * </p>
     *
     * @param request  the {@code Request} instance being processed.
     * @param response the {@code Response} instance being processed.
     * @param config   the login configuration describing how authentication should be performed
     */
    protected void forwardToErrorPage(Request request, Response response, LoginConfig config) {
        RequestDispatcher disp = this.context.getServletContext().getRequestDispatcher(config.getErrorPage());
        try {
            disp.forward(request.getRequest(), response.getResponse());
        } catch (Throwable t) {
            WebLogger.WEB_SECURITY_LOGGER.errorForwardingToErrorPage(t.getLocalizedMessage());
        }
    }
}