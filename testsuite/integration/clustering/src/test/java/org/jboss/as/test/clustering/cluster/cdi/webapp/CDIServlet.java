/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.cdi.webapp;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.jboss.as.test.clustering.cluster.ejb.stateful.bean.Incrementor;
import org.jboss.as.test.clustering.single.web.SimpleServlet;

/**
 * Note that the servlet is mapped to /simple using web.xml servlet-mapping overriding @WebServlet of the SimpleServlet.
 *
 * @author Tomas Remes
 * @author Radoslav Husar
 */
public class CDIServlet extends SimpleServlet {

    private static final long serialVersionUID = 5167789049451718160L;

    @Inject
    private Incrementor incrementor;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        this.increment(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        this.increment(request, response);
    }

    private void increment(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(true);
        response.addHeader(SESSION_ID_HEADER, session.getId());

        int value = this.incrementor.increment();

        response.setIntHeader(VALUE_HEADER, value);

        this.getServletContext().log(request.getRequestURI() + ", value = " + value);

        response.getWriter().write("Success");
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(true);
        response.addHeader(SESSION_ID_HEADER, session.getId());

        this.incrementor.reset();
    }
}
