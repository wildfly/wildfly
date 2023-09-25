/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.session;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;


/**
 */
@WebServlet(name = "SessionPersistenceServlet", urlPatterns = {"/SessionPersistenceServlet"})
public class SessionTestServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if(req.getParameter("invalidate") != null) {
            req.getSession().invalidate();
        } else {
            HttpSession session = req.getSession(true);
            Integer val = (Integer) session.getAttribute("val");
            if (val == null) {
                session.setAttribute("val", 0);
                resp.getWriter().print(0);
            } else {
                session.setAttribute("val", ++val);
                resp.getWriter().print(val);
            }
        }
    }

}
