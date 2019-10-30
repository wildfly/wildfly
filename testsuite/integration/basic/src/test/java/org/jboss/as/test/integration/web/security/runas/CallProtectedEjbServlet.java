/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.web.security.runas;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import javax.annotation.security.DeclareRoles;
import javax.ejb.EJB;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.logging.Logger;

/**
 * Protected servlet which calls protected EJB method {@link Hello#sayHello()}.
 *
 * @author olukas
 */
@WebServlet(CallProtectedEjbServlet.SERVLET_PATH)
@DeclareRoles({HelloBean.AUTHORIZED_ROLE, HelloBean.NOT_AUTHORIZED_ROLE})
public class CallProtectedEjbServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(CallProtectedEjbServlet.class);

    public static final String SERVLET_PATH = "/CallProtectedEjbServlet";
    public static final String FILE_PARAM = "file";
    public static final String INIT_METHOD_PASS = "init method passed.";
    public static final String INIT_METHOD_NOT_PASS = "init method did not pass.";
    public static final String DOGET_METHOD_PASS = "doGet method passed.";
    public static final String DOGET_METHOD_NOT_PASS = "doGet method did not pass.";
    public static final String DESTROY_METHOD_PASS = "destroy method passed.";
    public static final String DESTROY_METHOD_NOT_PASS = "destroy method did not pass.";

    private static String initMethodPass;
    private String filePath;

    @EJB
    private Hello ejb;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LOGGER.debug("Calling EJB protected method in doGet() method.");

        resp.setContentType("text/plain");
        filePath = req.getParameter(FILE_PARAM);

        try (PrintWriter writer = resp.getWriter()) {
            writer.println(initMethodPass);
            try {
                String callProtectedEJB = callProtectedEJB();
                writer.println(DOGET_METHOD_PASS + callProtectedEJB);
            } catch (Exception e) {
                writer.println(DOGET_METHOD_NOT_PASS);
                LOGGER.debug(e);
            }
        }
    }

    @Override
    public void init() throws ServletException {
        LOGGER.debug("Calling EJB protected method in init() method.");
        try {
            String callProtectedEJB = callProtectedEJB();
            initMethodPass = INIT_METHOD_PASS + callProtectedEJB;
        } catch (Exception e) {
            initMethodPass = INIT_METHOD_NOT_PASS;
            LOGGER.debug(e);
        }
    }

    @Override
    public void destroy() {
        LOGGER.debug("Calling EJB protected method in destroy() method.");
        if (filePath == null) {
            LOGGER.warnf("RunAs testing servlet was not called with '%s' parameter provided.", FILE_PARAM);
            try {
                String callProtectedEJB = callProtectedEJB();
                LOGGER.info(DESTROY_METHOD_PASS + callProtectedEJB);
            } catch (Exception e) {
                LOGGER.info(DESTROY_METHOD_NOT_PASS);
                LOGGER.debug(e);
            }
        } else {
            try (PrintWriter writer = new PrintWriter(filePath)) {
                LOGGER.infof("RunAs testing servlet will write result into file '%s'.", filePath);
                try {
                    String callProtectedEJB = callProtectedEJB();
                    writer.println(DESTROY_METHOD_PASS + callProtectedEJB);
                } catch (Exception e) {
                    writer.println(DESTROY_METHOD_NOT_PASS);
                    LOGGER.debug(e);
                }
            } catch (FileNotFoundException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    protected String callProtectedEJB() throws NamingException {
        return ejb.sayHello();
    }

}
