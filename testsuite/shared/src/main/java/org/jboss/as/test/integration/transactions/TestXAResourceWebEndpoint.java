/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.integration.transactions;

import org.jboss.logging.Logger;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(name="TestXAResourceWebEndpoint", urlPatterns={"/testxaresource/*"})
public class TestXAResourceWebEndpoint extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(TestXAResourceWebEndpoint.class);
    private static final String SUCCESS_RESPONSE = "SUCCESS";

    @EJB
    private TransactionCheckerSingleton checkerSingleton;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String requestPathInfo = request.getPathInfo();
        log.debug("request for servlet path " + request.getServletPath() + ", path info " + requestPathInfo);
        if (requestPathInfo.endsWith("/committed")) {
            out.print(checkerSingleton.getCommitted());
        } else if (requestPathInfo.endsWith("/rolledback")) {
            out.print(checkerSingleton.getRolledback());
        } else if (requestPathInfo.endsWith("/prepared")) {
            out.print(checkerSingleton.getPrepared());
        } else if (requestPathInfo.endsWith("/reset")) {
            checkerSingleton.resetAll();
            out.print(SUCCESS_RESPONSE);
        } else if (requestPathInfo.endsWith("/clearpersistency")) {
            new XidsPersister(PersistentTestXAResource.XIDS_PERSISTER_FILE_NAME).writeToDisk(null);
            out.print(SUCCESS_RESPONSE);
        } else if (requestPathInfo.endsWith("/clearprepared")) {
            TestXAResource.getPreparedXids().clear();
            out.print(SUCCESS_RESPONSE);
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
            out.print("No handler for path '" + requestPathInfo + "'");
        }
    }

}
