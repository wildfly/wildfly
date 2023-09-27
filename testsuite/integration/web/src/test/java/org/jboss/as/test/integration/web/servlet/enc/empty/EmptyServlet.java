/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.servlet.enc.empty;

import java.io.IOException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.Assert;


/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@WebServlet(name="SimpleServlet", urlPatterns={"/simple"})
public class EmptyServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            final InitialContext ic = new InitialContext();
            ic.lookup("java:comp/env");
            Assert.assertFalse(ic.list("java:comp/env").hasMore());
            resp.getWriter().write("ok");
            resp.getWriter().close();
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }

    }
}
