/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.security.securitycontext;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.handlers.ServletRequestContext;

public class MyDummyTokenHandler
        implements HttpHandler {

    @Override
    public void handleRequest(HttpServerExchange exchange)
            throws Exception {
        HttpServletRequest request = (HttpServletRequest) exchange
            .getAttachment(ServletRequestContext.ATTACHMENT_KEY)
            .getServletRequest();
        HttpServletResponse response = (HttpServletResponse) exchange
            .getAttachment(ServletRequestContext.ATTACHMENT_KEY)
            .getServletResponse();


        boolean login = false;
        try {
            login = Boolean.valueOf(request.getParameter("login"));
        } catch (Exception e) {
            //ignore
        }
        final String AUTHENTICATED = "authenticated";
        HttpSession session = request.getSession(false);
        if (login && (session == null || session.getAttribute(AUTHENTICATED) == null)) {
            request.login("testuser", "testpassword");
            session = session == null ? request.getSession() : session;
            session.setAttribute(AUTHENTICATED, AUTHENTICATED);
        }
    }

}
