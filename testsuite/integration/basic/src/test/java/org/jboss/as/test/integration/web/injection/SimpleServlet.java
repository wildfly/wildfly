/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.injection;

import java.io.IOException;
import java.io.Writer;

import jakarta.ejb.EJB;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
