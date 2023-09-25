/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.security.common.servlets;

import jakarta.annotation.security.DeclareRoles;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;

/**
 * Protected version of {@link SimpleServlet}. Only {@value #ALLOWED_ROLE} role has access right.
 *
 * @author Josef Cacek
 */
@DeclareRoles({ SimpleSecuredServlet.ALLOWED_ROLE })
@ServletSecurity(@HttpConstraint(rolesAllowed = { SimpleSecuredServlet.ALLOWED_ROLE }))
@WebServlet(SimpleSecuredServlet.SERVLET_PATH)
public class SimpleSecuredServlet extends SimpleServlet {

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;
    public static final String SERVLET_PATH = "/SimpleSecuredServlet";
    public static final String ALLOWED_ROLE = "JBossAdmin";
}
