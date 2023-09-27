/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.web.context;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * @author Paul Ferraro
 */
@WebServlet(urlPatterns = { LogoutServlet.SERVLET_PATH })
public class LogoutServlet extends HttpServlet {
    private static final long serialVersionUID = -1574442587573954813L;
    private static final String SERVLET_NAME = "logout";
    static final String SERVLET_PATH = "/" + SERVLET_NAME;

    public static URI createURI(URL baseURL, String conversation) throws URISyntaxException {
        return baseURL.toURI().resolve(new StringBuilder(SERVLET_NAME).append('?').append(ConversationServlet.CONVERSATION_ID).append('=').append(conversation).toString());
    }

    @Inject
    private ConversationBean bean;

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader(ConversationServlet.COUNT_HEADER, String.valueOf(this.bean.increment()));
        response.setHeader(ConversationServlet.CONVERSATION_ID, this.bean.getConversationId());
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
