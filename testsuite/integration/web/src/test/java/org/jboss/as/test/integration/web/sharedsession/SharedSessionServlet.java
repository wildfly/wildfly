/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.sharedsession;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;


/**
 */
@WebServlet(name = "SharedSessionServlet", urlPatterns = {"/SharedSessionServlet"})
public class SharedSessionServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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
