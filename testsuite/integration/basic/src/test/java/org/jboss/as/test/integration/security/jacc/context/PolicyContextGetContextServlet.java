/*
Copyright 2018 Red Hat, Inc.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
  http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package org.jboss.as.test.integration.security.jacc.context;

import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Servlet for test {@link PolicyContextGetContextTestCase}
 * @author <a href="mailto:padamec@redhat.com">Petr Adamec</a>
 */
@WebServlet(urlPatterns = {PolicyContextGetContextServlet.SERVLET_PATH})
public class PolicyContextGetContextServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public static final String SERVLET_PATH = "/policy-context-get-context";
    public static final String SUCCESS_MESSAGE = "PolicyContext.getContext returns object";

    /**
     * Call method {@link PolicyContext#getContext(String key) getContext(String key)} with parameter <i>javax.security.auth.Subject.container</i>.
     * If response is not null, add {@link #SUCCESS_MESSAGE} to http response.
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            Object obj = PolicyContext.getContext("javax.security.auth.Subject.container");
            if (obj != null) { response.getWriter().write(SUCCESS_MESSAGE); }
        } catch (PolicyContextException e) {
            throw new ServletException("Error retrieving request: " + e.getMessage(), e);
        }
    }
}
