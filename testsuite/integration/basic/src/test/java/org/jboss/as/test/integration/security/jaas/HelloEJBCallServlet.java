/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.security.jaas;

import java.io.IOException;
import java.io.PrintWriter;

import javax.annotation.security.DeclareRoles;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.as.test.integration.security.common.ejb3.Hello;
import org.jboss.as.test.integration.security.common.ejb3.HelloBean;
import org.jboss.logging.Logger;

/**
 * Servlet which handles GET requests. It calls remote EJB3 method {@link Hello#sayHello()} and returns it as a plain text
 * response. JNDI lookup of the Hello bean is based on provided {@value #PARAM_JNDI_NAME} request parameter.
 *
 * @author Josef Cacek
 */
@DeclareRoles({ HelloBean.ROLE_ALLOWED })
@ServletSecurity(@HttpConstraint(rolesAllowed = { HelloBean.ROLE_ALLOWED }))
@WebServlet(HelloEJBCallServlet.SERVLET_PATH)
public class HelloEJBCallServlet extends HttpServlet {

    private static Logger LOGGER = Logger.getLogger(HelloEJBCallServlet.class);

    private static final long serialVersionUID = 1L;

    public static final String SERVLET_PATH = "/hello";

    public static final String PARAM_JNDI_NAME = "jndiName";

    /**
     * Writes simple text response.
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        final PrintWriter writer = resp.getWriter();
        final String jndiName = req.getParameter(PARAM_JNDI_NAME);
        try {
            final Context ctx = new InitialContext();
            final Hello ejbObject = (Hello) ctx.lookup(jndiName);
            final String msg = ejbObject.sayHello();
            LOGGER.trace(msg);
            writer.append(msg);
            ctx.close();
        } catch (NamingException ex) {
            LOGGER.warn("Unable to get EJB.", ex);
            writer.append(ex.getMessage());
        }
        writer.close();
    }
}
