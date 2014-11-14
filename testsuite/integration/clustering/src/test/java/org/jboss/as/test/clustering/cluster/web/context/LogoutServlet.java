/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.web.context;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

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
