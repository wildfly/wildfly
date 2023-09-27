/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.packaging.war;

import java.io.IOException;
import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


/**
 * @author Ondrej Chaloupka
 */
public class Servlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @EJB
    JarBean jarBean;

    @EJB
    WarBean warBean;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Object attr = req.getParameter("archive");

        if ("jar".equals(attr)) {
            try {
                resp.getOutputStream().print(jarBean.checkMe());
            } catch (Exception e) {
                resp.getOutputStream().print("error");
            }
        }
        if ("war".equals(attr)) {
            try {
                resp.getOutputStream().print(warBean.checkMe());
            } catch (Exception e) {
                resp.getOutputStream().print("error");
            }
        }
    }

}
