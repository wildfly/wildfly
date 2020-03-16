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
package org.jboss.as.test.clustering.cluster.cdi;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.jboss.as.test.clustering.cluster.ejb.stateful.bean.Incrementor;
import org.jboss.as.test.clustering.single.web.SimpleServlet;

/**
 * Note that the servlet is mapped to /simple using web.xml servlet-mapping overriding @WebServlet of the SimpleServlet.
 *
 * @author Tomas Remes
 * @author Radoslav Husar
 */
public class CdiServlet extends SimpleServlet {

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
