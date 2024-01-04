/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.security.runas;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RunAs;
import jakarta.servlet.annotation.WebServlet;

/**
 * RunAs annotated servlet which calls protected EJB method {@link Hello#sayHello()}. Role used in RunAs in not correct role for
 * protected EJB method.
 *
 * @author olukas
 */
@WebServlet(RunAsUserServlet.SERVLET_PATH)
@DeclareRoles({HelloBean.AUTHORIZED_ROLE, HelloBean.NOT_AUTHORIZED_ROLE})
@RunAs(HelloBean.NOT_AUTHORIZED_ROLE)
public class RunAsUserServlet extends CallProtectedEjbServlet {

    private static final long serialVersionUID = 1L;

    public static final String SERVLET_PATH = "/RunAsUserServlet";

}
