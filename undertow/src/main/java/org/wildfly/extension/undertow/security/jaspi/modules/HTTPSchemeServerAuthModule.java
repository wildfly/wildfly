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

package org.wildfly.extension.undertow.security.jaspi.modules;

import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.SecurityContext;
import io.undertow.security.impl.BasicAuthenticationMechanism;
import io.undertow.security.impl.ClientCertAuthenticationMechanism;
import io.undertow.security.impl.DigestAuthenticationMechanism;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.api.LoginConfig;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.servlet.handlers.security.ServletFormAuthenticationMechanism;
import org.wildfly.extension.undertow.security.jaspi.JASPIAuthenticationMechanism;

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
import java.util.Map;

import static io.undertow.security.api.AuthenticationMechanism.AuthenticationMechanismOutcome.*;
import static javax.security.auth.message.AuthStatus.*;
import static javax.servlet.http.HttpServletRequest.*;
import static org.wildfly.extension.undertow.UndertowLogger.*;

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
        HttpServerExchange exchange = (HttpServerExchange) messageInfo.getMap().get(JASPIAuthenticationMechanism.HTTP_SERVER_EXCHANGE_ATTACHMENT_KEY);
        SecurityContext securityContext = (SecurityContext) messageInfo.getMap().get(JASPIAuthenticationMechanism.SECURITY_CONTEXT_ATTACHMENT_KEY);

        try {
            AuthenticationMechanism mechanism = getAuthenticationMechanism(exchange);

            if (!AUTHENTICATED.equals(mechanism.authenticate(exchange, securityContext))) {
                AuthenticationMechanism.ChallengeResult challengeResult = mechanism.sendChallenge(exchange, securityContext);

                exchange.setResponseCode(challengeResult.getDesiredResponseCode());

                return SEND_CONTINUE;
            }
        } catch (Exception e) {
            ROOT_LOGGER.debug(e);
            throw new AuthException("Could not validateRequest using mechanism [" + BasicAuthenticationMechanism.class.getName() + "].");
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

    protected AuthenticationMechanism getAuthenticationMechanism(HttpServerExchange exchange) {
        if (this.authenticationMechanism == null) {
            this.authenticationMechanism = doCreateAuthenticationMechanism(exchange);
        }
        return this.authenticationMechanism;
    }

    protected AuthenticationMechanism doCreateAuthenticationMechanism(HttpServerExchange exchange) {
        ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        LoginConfig loginConfig = servletRequestContext.getDeployment().getDeploymentInfo().getLoginConfig();

        String desiredAuthMethod = loginConfig.getAuthMethod().toUpperCase();

        if (BASIC_AUTH.equals(desiredAuthMethod)) {
            return new BasicAuthenticationMechanism(loginConfig.getRealmName());
        } else if (DIGEST_AUTH.equals(desiredAuthMethod)) {
            return new DigestAuthenticationMechanism(loginConfig.getRealmName(), servletRequestContext.getCurrentServetContext().getContextPath(), DIGEST_AUTH);
        } else if (FORM_AUTH.equals(desiredAuthMethod)) {
            return new ServletFormAuthenticationMechanism(FORM_AUTH, loginConfig.getLoginPage(), loginConfig.getErrorPage());
        } else if (CLIENT_CERT_AUTH.equals(desiredAuthMethod)) {
            return new ClientCertAuthenticationMechanism();
        } else {
            throw new RuntimeException("Invalid authentication mechanism [" + loginConfig.getAuthMethod() + "].");
        }
    }

}