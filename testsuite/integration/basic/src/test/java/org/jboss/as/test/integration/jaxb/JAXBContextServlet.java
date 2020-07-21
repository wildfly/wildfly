/*
 * JBoss, Home of Professional Open Source
 * Copyright 2020 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.test.integration.jaxb;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
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
