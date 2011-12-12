/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.jaxr;

import org.jboss.logging.Logger;

import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.registry.ConnectionFactory;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Tests the JAXR connection factory bound to JNDI
 *
 * @author Thomas.Diesler@jboss.com
 * @since 26-Oct-2011
 */
public class JaxrServlet extends HttpServlet
{
    static Logger log = Logger.getLogger(JaxrServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String method = req.getParameter("method");
        PrintWriter writer = res.getWriter();
        try {
            ConnectionFactory factory;
            if ("lookup".equals(method)) {
                String lookup = "java:jboss/jaxr/ConnectionFactory";
                InitialContext context = new InitialContext();
                factory = (ConnectionFactory) context.lookup(lookup);
            }
            else if ("new".equals(method)) {
                factory = ConnectionFactory.newInstance();
            } else {
                throw new IllegalArgumentException("Invalid method: " + method);
            }
            log.infof("ConnectionFactory at '%s' => %s", method, factory);
            writer.println(factory.getClass().getName());
        } catch (Exception ex) {
            ex.printStackTrace(writer);
        } finally {
            writer.close();
        }
    }
}
