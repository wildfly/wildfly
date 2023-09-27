/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.web.security.tg;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.ServletSecurity.TransportGuarantee;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Testing servlet which enables transport guarantee security constraint.
 *
 * @author <a href="mailto:pskopek@redhat.com">Peter Skopek</a>
 */
@WebServlet(name = "TG_MIXED_servlet", urlPatterns = {TransportGuaranteeMixedServlet.servletContext}, loadOnStartup = 1)
@ServletSecurity(@HttpConstraint(rolesAllowed = {"gooduser"}, transportGuarantee = TransportGuarantee.NONE))
public class TransportGuaranteeMixedServlet extends HttpServlet {

    private static final long serialVersionUID = 3L;

    public static final String servletContext = "/tg_mixed/srv";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().write("TransportGuaranteedGet");
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().write("TransportGuaranteedPost");
    }

}

