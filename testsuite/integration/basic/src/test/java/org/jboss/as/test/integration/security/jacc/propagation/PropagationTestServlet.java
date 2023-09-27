/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.security.jacc.propagation;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.annotation.security.DeclareRoles;
import jakarta.ejb.EJBAccessException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
     * @see jakarta.servlet.http.HttpServlet#doGet(jakarta.servlet.http.HttpServletRequest, jakarta.servlet.http.HttpServletResponse)
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
