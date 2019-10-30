/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow.security.jaspi.modules;

import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.SecurityContext;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.handlers.ServletRequestContext;
import org.wildfly.extension.undertow.logging.UndertowLogger;
import org.wildfly.extension.undertow.security.jaspi.JASPICAuthenticationMechanism;

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
import java.util.List;
import java.util.Map;

import static io.undertow.security.api.AuthenticationMechanism.AuthenticationMechanismOutcome.AUTHENTICATED;
import static io.undertow.security.api.AuthenticationMechanism.AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
import static javax.security.auth.message.AuthStatus.SEND_CONTINUE;
import static javax.security.auth.message.AuthStatus.SUCCESS;

/**
 * <p> This class implements a JASPI {@code ServerAuthModule} that handles the standards HTTP Authentication
 * Schemes.</p>
 *
 * @author Pedro Igor
 */
public class HTTPSchemeServerAuthModule implements ServerAuthModule {

    private final String securityDomain;
    private AuthenticationMechanism authenticationMechanism;

    public HTTPSchemeServerAuthModule(String securityDomain) {
        this.securityDomain = securityDomain;
    }

    @Override
    public void initialize(final MessagePolicy messagePolicy, final MessagePolicy messagePolicy2, final CallbackHandler callbackHandler, final Map map) throws AuthException {
    }

    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject)
            throws AuthException {
        // do nothing, just return SUCCESS.
        return SUCCESS;
    }

    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject)
            throws AuthException {
        HttpServerExchange exchange = (HttpServerExchange) messageInfo.getMap().get(JASPICAuthenticationMechanism.HTTP_SERVER_EXCHANGE_ATTACHMENT_KEY);
        SecurityContext securityContext = (SecurityContext) messageInfo.getMap().get(JASPICAuthenticationMechanism.SECURITY_CONTEXT_ATTACHMENT_KEY);
        ServletRequestContext src = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        List<AuthenticationMechanism> mechanisms = src.getDeployment().getAuthenticationMechanisms();

        try {
            boolean success = false;
            for (AuthenticationMechanism mechanism : mechanisms) {
                AuthenticationMechanism.AuthenticationMechanismOutcome result = mechanism.authenticate(exchange, securityContext);
                if (result == AUTHENTICATED) {
                    success = true;
                    break;
                } else if (result == NOT_AUTHENTICATED) {
                    break;
                }
            }

            if (!success) {
                String mandatory = (String) messageInfo.getMap().get("javax.security.auth.message.MessagePolicy.isMandatory");
                if(mandatory != null && mandatory.toLowerCase().equals("false")) {
                    return SUCCESS;
                } else {
                    for (AuthenticationMechanism mechanism : mechanisms) {
                        AuthenticationMechanism.ChallengeResult challengeResult = mechanism.sendChallenge(exchange, securityContext);
                        if (challengeResult.getDesiredResponseCode() != null) {
                            exchange.setResponseCode(challengeResult.getDesiredResponseCode());
                        }
                        if (exchange.isResponseComplete()) {
                            break;
                        }
                    }
                    return SEND_CONTINUE;
                }
            }
        } catch (Exception e) {
            UndertowLogger.ROOT_LOGGER.debug(e);
            throw new AuthException("Could not validateRequest using mechanisms [" + mechanisms + ".");
        }

        return SUCCESS;
    }

    @Override
    public Class[] getSupportedMessageTypes() {
        return new Class[]{ServletRequest.class, ServletResponse.class,
                HttpServletRequest.class, HttpServletResponse.class};
    }

    @Override
    public void cleanSubject(final MessageInfo messageInfo, final Subject subject) throws AuthException {
        //TODO: is necessary to clean the subject here ?
    }
}
