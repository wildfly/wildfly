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

/**
 * @author Paul Ferraro
 */
@WebServlet(urlPatterns = { ConversationServlet.SERVLET_PATH })
public class ConversationServlet extends HttpServlet {
    private static final long serialVersionUID = -1574442587573954813L;
    private static final String SERVLET_NAME = "conversation";
    static final String SERVLET_PATH = "/" + SERVLET_NAME;
    public static final String COUNT_HEADER = "count";
    public static final String CONVERSATION_ID = "cid";

    public static URI createURI(URL baseURL) throws URISyntaxException {
        return baseURL.toURI().resolve(SERVLET_NAME);
    }

    public static URI createURI(URL baseURL, String conversation) throws URISyntaxException {
        return baseURL.toURI().resolve(SERVLET_NAME + '?' + CONVERSATION_ID + '=' + conversation);
    }

    @Inject
    private ConversationBean bean;

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader(COUNT_HEADER, String.valueOf(this.bean.increment()));
        response.setHeader(CONVERSATION_ID, this.bean.getConversationId());
    }
}
