/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

import org.jboss.logging.Logger;

/**
 * A servlet that initiates a transaction, uses a RequestDispatcher to include or forward to {@link TxStatusServlet}
 * and then optionally commits the transaction. Used to test that the transaction propagates and that failure to
 * commit it is properly detected.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
@WebServlet(name = "TxControlServlet", urlPatterns = "/" + TxControlServlet.URL_PATTERN)
public class TxControlServlet extends HttpServlet {

    private static final long serialVersionUID = -853278446594804509L;

    private static Logger log = Logger.getLogger(TxControlServlet.class);

    /** The name of the context to which requests are forwarded */
    private static final String forwardContext = "/tx-status";
    private static final String forwardPath = TxStatusServlet.URL_PATTERN;
    static final String URL_PATTERN = "TxControlServlet";
    static final String INNER_STATUS_HEADER = "X-Inner-Transaction-Status";
    static final String OUTER_STATUS_HEADER = "X-Outer-Transaction-Status";

    /**
     * Lookup the UserTransaction and begin a transaction.
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (log.isTraceEnabled()) {
            log.trace("[" + forwardContext + "], PathInfo: " + request.getPathInfo() + ", QueryString: "
                    + request.getQueryString() + ", ContextPath: " + request.getContextPath() + ", HeaderNames: "
                    + request.getHeaderNames() + ", isCommitted: " + response.isCommitted());
        }

        String includeParam = request.getParameter("include");
        if (includeParam == null)
            throw new IllegalStateException("No include parameter seen");
        boolean include = Boolean.valueOf(includeParam);

        String commitParam = request.getParameter("commit");
        if (commitParam == null)
            throw new IllegalStateException("No commit parameter seen");
        boolean commit = Boolean.valueOf(commitParam);

        UserTransaction transaction;
        try {
            transaction = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
            transaction.begin();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ServletContext sc = getServletContext().getContext(forwardContext);
        if (sc != null) {
//            if (log.isTraceEnabled())
                log.trace("Found ServletContext for: " + forwardContext);
            RequestDispatcher rd = sc.getRequestDispatcher(forwardPath);
            if (rd != null) {
//                if (log.isTraceEnabled())
                    log.trace("Found RequestDispatcher for: " + forwardPath);
                if (include) {
                    rd.include(request, response);
                } else {
                    rd.forward(request, response);
                }

                // Get the tx status that TxStatusServlet saw
                Integer status = (Integer) request.getAttribute(TxStatusServlet.ATTRIBUTE);
                if (status == null) {
                    throw new ServletException("No transaction status");
                }
                if (include) {
                    // We can still write to the response w/ an include, so pass the status to the client
                    response.setHeader(INNER_STATUS_HEADER, status.toString());
                } else if (status.intValue() != Status.STATUS_ACTIVE) {
                    throw new ServletException("Status is " + status);
                }

            }  else {
                throw new ServletException("No RequestDispatcher for: " + forwardContext + forwardPath);
            }
        } else {
            throw new ServletException("No ServletContext for: " + forwardContext);
        }


        try {
            // Get the tx status now
            int ourStatus = transaction.getStatus();
            if (include) {
                // We can still write to the response w/ an include, so pass the status to the client
                response.setHeader(OUTER_STATUS_HEADER, String.valueOf(ourStatus));
            } else if (ourStatus != Status.STATUS_ACTIVE) {
                throw new ServletException("Status is " + ourStatus);
            }
            if (commit) {
                transaction.commit();
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }

    }

}
