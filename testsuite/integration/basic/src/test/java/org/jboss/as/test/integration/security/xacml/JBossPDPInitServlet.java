/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.security.xacml;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

/**
 * A simple servlet handles GET requests and tries to initialize a JBossPDP using {@link JBossPDPServiceBean}.
 *
 * @author Josef Cacek
 */
@WebServlet(JBossPDPInitServlet.SERVLET_PATH)
public class JBossPDPInitServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(JBossPDPInitServlet.class);

    /**
     * The SERVLET_PATH used in {@link WebServlet} annotation
     */
    public static final String SERVLET_PATH = "/pdpInitServlet";

    public static final String RESPONSE_OK = "OK";
    public static final String RESPONSE_FAILED = "FAILED";

    // Protected methods -----------------------------------------------------

    /**
     * GET request initializes PDPServiceBean and checks if the JBossPDP is initialized too. Writes {@value #RESPONSE_OK} to the
     * response if everything is fine, {@value #RESPONSE_FAILED} otherwise.
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        boolean ok = false;
        try {
            //try to create service bean
            final JBossPDPServiceBean pdpService = new JBossPDPServiceBean();
            ok = pdpService.getJBossPDP() != null;
        } catch (Exception e) {
            LOGGER.error("Retrieving JBossPDP failed", e);
        }
        resp.setContentType("text/plain");
        PrintWriter writer = resp.getWriter();
        writer.print(ok ? RESPONSE_OK : RESPONSE_FAILED);
        writer.close();
    }
}
