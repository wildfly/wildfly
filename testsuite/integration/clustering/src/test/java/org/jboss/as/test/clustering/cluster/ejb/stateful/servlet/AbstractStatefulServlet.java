/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.ejb.stateful.servlet;

import java.io.IOException;
import java.util.function.BiConsumer;

import jakarta.ejb.NoSuchEJBException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.jboss.as.test.clustering.cluster.ejb.stateful.bean.Incrementor;
import org.wildfly.common.function.ExceptionFunction;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractStatefulServlet extends HttpServlet implements ExceptionFunction<HttpServletRequest, Incrementor, ServletException>, BiConsumer<HttpSession, Incrementor> {
    private static final long serialVersionUID = 6443963367894900618L;

    public static final String MODULE = "module";
    public static final String COUNT = "count";
    public static final String BEAN = "bean";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(true);
        Incrementor incrementor = this.apply(request);
        try {
            response.setHeader(COUNT, String.valueOf(incrementor.increment()));
            this.accept(session, incrementor);
        } catch (NoSuchEJBException e) {
            response.setHeader(COUNT, String.valueOf(0));
            this.accept(session, null);
        }
        response.getWriter().write("Success");
    }
}
