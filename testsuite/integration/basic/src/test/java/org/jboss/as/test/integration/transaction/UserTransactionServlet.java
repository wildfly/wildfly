/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.transaction;

import java.io.IOException;
import java.io.Writer;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.UserTransaction;

/**
 * @author Stuart Douglas
 */
@WebServlet(name = "UserTransactionServlet", urlPatterns = {"/simple"})
public class UserTransactionServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            final UserTransaction transaction = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
            Writer writer = resp.getWriter();
            writer.write(Boolean.valueOf(transaction != null).toString());
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }

    }
}
