/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.security.runas;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RunAs;
import jakarta.servlet.annotation.WebServlet;

/**
 * RunAs annotated servlet which calls protected EJB method {@link Hello#sayHello()}.
 *
 * @author olukas
 */
@WebServlet(RunAsAdminServlet.SERVLET_PATH)
@DeclareRoles({HelloBean.AUTHORIZED_ROLE, HelloBean.NOT_AUTHORIZED_ROLE})
@RunAs(HelloBean.AUTHORIZED_ROLE)
public class RunAsAdminServlet extends CallProtectedEjbServlet {

    private static final long serialVersionUID = 1L;

    public static final String SERVLET_PATH = "/RunAsAdminServlet";

}
