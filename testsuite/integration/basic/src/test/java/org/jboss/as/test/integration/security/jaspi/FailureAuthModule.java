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
package org.jboss.as.test.integration.security.jaspi;

import java.io.IOException;

import javax.security.auth.Subject;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.Response;
import org.apache.log4j.Logger;
import org.jboss.as.web.security.jaspi.modules.WebServerAuthModule;

/**
 * An {@link javax.security.auth.message.module.ServerAuthModule} implementation which always sends
 * {@link HttpServletResponse#SC_UNAUTHORIZED} when {@link #validateRequest(MessageInfo, Subject, Subject)} is called.
 * 
 * @author Josef Cacek
 */
public class FailureAuthModule extends WebServerAuthModule {
    private static Logger LOGGER = Logger.getLogger(FailureAuthModule.class);

    // Public methods --------------------------------------------------------

    /**
     * Returns {@link AuthStatus#SUCCESS}.
     * 
     * @param messageInfo
     * @param serviceSubject
     * @return
     * @throws AuthException
     * @see org.jboss.as.web.security.jaspi.modules.WebServerAuthModule#secureResponse(javax.security.auth.message.MessageInfo,
     *      javax.security.auth.Subject)
     */
    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) throws AuthException {
        LOGGER.debug("secureResponse()");
        return AuthStatus.SUCCESS;
    }

    /**
     * Sends HTTP error response {@link HttpServletResponse#SC_UNAUTHORIZED} and returns {@link AuthStatus#FAILURE}.
     * 
     * @param messageInfo
     * @param clientSubject
     * @param serviceSubject
     * @return
     * @throws AuthException
     * @see org.jboss.as.web.security.jaspi.modules.WebServerAuthModule#validateRequest(javax.security.auth.message.MessageInfo,
     *      javax.security.auth.Subject, javax.security.auth.Subject)
     */
    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject)
            throws AuthException {
        LOGGER.debug("validateRequest()");
        final Response response = (Response) messageInfo.getResponseMessage();
        try {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        } catch (IOException e) {
            LOGGER.error("Error", e);
        }
        return AuthStatus.FAILURE;
    }
}
