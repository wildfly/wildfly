/*
 * Copyright 2020 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.test.elytron.intermediate;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet that uses the LoginCounterLoginModule to return the logged user
 * and the times the login module was called (timesCalled). The format
 * of the response is {username}:{timesCalled} in "text/plain" format.
 *
 * @author rmartinc
 */
@WebServlet(name = "PrincipalPrintingServlet", urlPatterns = { PrincipalCounterServlet.SERVLET_PATH })
public class PrincipalCounterServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public static final String SERVLET_PATH = "/countPrincipal";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        try (PrintWriter writer = resp.getWriter()) {
            final String username = req.getUserPrincipal() == null? "null" : req.getUserPrincipal().getName();
            writer.write(username + ":" + LoginCounterLoginModule.getTimesCalled());
        }
    }
}
