/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxb;

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.jboss.as.test.integration.jaxb.bindings.PurchaseOrderType;

/**
 * <p>Servlet that shows the JAXBContext retrieved.</p>
 *
 * @author rmartinc
 */
@WebServlet (urlPatterns = JAXBContextServlet.URL_PATTERN)
public class JAXBContextServlet extends HttpServlet {

    public static final String URL_PATTERN = "/jaxbContextServlet";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            JAXBContext ctx = JAXBContext.newInstance(PurchaseOrderType.class);
            resp.setContentType("text/plain;charset=UTF-8");
            try (PrintWriter out = resp.getWriter()) {
                out.print(ctx);
            }
        } catch (JAXBException e) {
            throw new ServletException(e);
        }
    }
}
