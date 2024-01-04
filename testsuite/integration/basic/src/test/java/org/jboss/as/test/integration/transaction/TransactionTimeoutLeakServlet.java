/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.transaction;

import java.io.IOException;
import java.io.Writer;
import javax.naming.InitialContext;
import javax.transaction.xa.XAResource;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.TransactionManager;
import org.jboss.as.test.integration.transactions.TestXAResource;

@WebServlet(name = "TransactionTimeoutLeakServlet", urlPatterns = {"/timeout"})
public class TransactionTimeoutLeakServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            final TransactionManager tm = (TransactionManager) new InitialContext().lookup("java:/TransactionManager");
            final String second = req.getParameter("second");
            if (second != null) {
                final int timeoutValue = Integer.parseInt(second);
                if (timeoutValue > 0) {
                    tm.setTransactionTimeout(timeoutValue);
                }
            }

            XAResource xaer = new TestXAResource();
            tm.begin();
            tm.getTransaction().enlistResource(xaer);
            int effectiveTimeout = xaer.getTransactionTimeout();
            tm.commit();

            Writer writer = resp.getWriter();
            writer.write(String.valueOf(effectiveTimeout));
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
