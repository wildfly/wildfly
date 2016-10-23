/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.web.tx;

import java.io.IOException;

import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

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
