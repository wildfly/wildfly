/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.web.injection;

import java.io.IOException;
import java.io.Writer;

import javax.ejb.EJB;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @author Eduardo Martins
 */
@WebServlet(name = "SimpleServlet", urlPatterns = { "/simple" })
public class SimpleServlet extends HttpServlet {
    @EJB
    private SimpleStatelessSessionBean bean;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        testAS7_5347(resp);

        String msg = req.getParameter("input");
        Writer writer = resp.getWriter();
        writer.write(bean.echo(msg));
    }

    private void testAS7_5347(HttpServletResponse resp) throws IOException {
        // java:module includes child EJBContext, which can't be looked up on a servlet, yet list() on this context must not
        // fail, more info at AS7-5347
        try {
            new InitialContext().list("java:module");
        } catch (NamingException e) {
            resp.getWriter().write("AS7-5347 solution check failed");
        }
    }
}
