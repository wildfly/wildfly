/*
Copyright 2018 Red Hat, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package org.jboss.as.test.integration.security.securitycontext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

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
            session.putValue(AUTHENTICATED, AUTHENTICATED);
        }
    }

}
