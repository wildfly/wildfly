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
package org.jboss.as.test.integration.security.jacc.propagation;

import java.io.IOException;
import java.io.PrintWriter;

import javax.annotation.security.DeclareRoles;
import javax.ejb.EJBAccessException;
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

import org.jboss.logging.Logger;

/**
 * Servlet for testing JACC authorization propagation. It gets 2 request parameters:
 * <ul>
 * <li>{@value #PARAM_BEAN_NAME} - bean name used to JNDI lookup of {@link Manage} interface implementation
 * <li>{@value #PARAM_METHOD_NAME} - the value should be one of {@value #METHOD_NAME_ADMIN}, {@value #METHOD_NAME_MANAGE},
 * {@value #METHOD_NAME_USER}
 * </ul>
 *
 * @author Josef Cacek
 */
@DeclareRoles({Manage.ROLE_ADMIN, Manage.ROLE_MANAGER, Manage.ROLE_USER})
@ServletSecurity(@HttpConstraint(rolesAllowed = {Manage.ROLE_ADMIN, Manage.ROLE_MANAGER, Manage.ROLE_USER}))
@WebServlet(PropagationTestServlet.SERVLET_PATH)
public class PropagationTestServlet extends HttpServlet {

    private static Logger LOGGER = Logger.getLogger(PropagationTestServlet.class);

    private static final long serialVersionUID = 1L;

    public static final String SERVLET_PATH = "/propagation";

    public static final String PARAM_BEAN_NAME = "beanName";
    public static final String PARAM_METHOD_NAME = "methodName";

    public static final String METHOD_NAME_ADMIN = "admin";
    public static final String METHOD_NAME_MANAGE = "manage";
    public static final String METHOD_NAME_WORK = "work";

    public static final String RESULT_EJB_ACCESS_EXCEPTION = "EjbAccessException";

    /**
     * Tests access to EJBs implementing {@link Manage} interface.
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
        final String beanName = req.getParameter(PARAM_BEAN_NAME);
        final String methodName = req.getParameter(PARAM_METHOD_NAME);
        Context ctx = null;
        try {
            ctx = new InitialContext();
            final Manage manageBean = (Manage) ctx.lookup("java:app/" + Manage.TEST_NAME + "/" + beanName);
            String msg = null;
            if (METHOD_NAME_ADMIN.equals(methodName)) {
                msg = manageBean.admin();
            } else if (METHOD_NAME_MANAGE.equals(methodName)) {
                msg = manageBean.manage();
            } else if (METHOD_NAME_WORK.equals(methodName)) {
                msg = manageBean.work();
            } else {
                msg = "Unknown method: " + methodName;
            }
            writer.append(msg);
        } catch (EJBAccessException e) {
            //expected state in this servlet
            writer.append(RESULT_EJB_ACCESS_EXCEPTION);
        } catch (Exception e) {
            LOGGER.error("EJB Call failed", e);
            e.printStackTrace(writer);
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException e) {
                    LOGGER.error("Error", e);
                }
            }
        }
        writer.close();
    }
}
