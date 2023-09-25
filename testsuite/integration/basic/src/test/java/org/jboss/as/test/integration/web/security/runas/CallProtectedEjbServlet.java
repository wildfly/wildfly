/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.security.runas;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import jakarta.annotation.security.DeclareRoles;
import jakarta.ejb.EJB;
import javax.naming.NamingException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
