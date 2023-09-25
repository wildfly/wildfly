/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.web.tx;

import java.io.IOException;

import javax.naming.InitialContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Status;
import jakarta.transaction.UserTransaction;

import org.jboss.logging.Logger;

/**
 * A servlet that checks for the status of a propagated transaction, failing if it is not {@link Status#STATUS_ACTIVE}.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
@WebServlet(name = "TxStatusServlet", urlPatterns = TxStatusServlet.URL_PATTERN)
public class TxStatusServlet extends HttpServlet {
    private static Logger log = Logger.getLogger(TxStatusServlet.class);
    static final String URL_PATTERN = "/TxStatusServlet";
    static final String ATTRIBUTE = "status";

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        log.trace("In TxStatusServlet");
        UserTransaction transaction;
        try {
            transaction = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
            int status = transaction.getStatus();
            log.trace("Transaction status is " + status);
            request.setAttribute(ATTRIBUTE, Integer.valueOf(status));
        } catch (Exception e) {
            log.error("Failed retrieving transaction status", e);
            throw new RuntimeException(e);
        }
    }
}
