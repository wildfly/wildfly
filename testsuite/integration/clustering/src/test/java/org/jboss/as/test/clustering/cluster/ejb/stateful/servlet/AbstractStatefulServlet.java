/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
