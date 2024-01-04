/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.injection.ejbref.lookup;

import java.io.IOException;
import jakarta.ejb.EJB;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/test2")
public class SecondTestServlet extends HttpServlet {

    @EJB(lookup = "ejb:ejb-test-ear/ejb/RemoteInterfaceBean!org.jboss.as.test.integration.ejb.injection.ejbref.lookup.RemoteInterface")
    RemoteInterface remoteBean;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        write(response, String.valueOf(remoteBean.ping()));
    }

    private static void write(HttpServletResponse writer, String message) {
        try {
            writer.getWriter().write(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
