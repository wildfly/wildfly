/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.web.security.jaspi;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A test AuthModule.
 *
 * Always throws an AuthException on {@link FailingAuthModule#validateRequest} call
 *
 * @author <a href="mailto:bspyrkos@redhat.com">Bartosz Spyrko-Smietanko</a>
 */
public class FailingAuthModule implements ServerAuthModule {

    public FailingAuthModule(String securityDomain) {

    }

    @Override
    public Class[] getSupportedMessageTypes() {
        return new Class[]{ServletRequest.class, ServletResponse.class,
                HttpServletRequest.class, HttpServletResponse.class};
    }

    @Override
    public void initialize(MessagePolicy messagePolicy, MessagePolicy messagePolicy1, CallbackHandler callbackHandler, Map map) throws AuthException {

    }

    @Override
    public void cleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {

    }

    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject subject) throws AuthException {
        return null;
    }

    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject subject, Subject subject1) throws AuthException {
        throw new AuthException("I always fail!!");
    }
}
