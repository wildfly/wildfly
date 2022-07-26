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

package org.wildfly.test.integration.web.annotationsmodule;

import org.jboss.as.naming.InitialContext;

import jakarta.annotation.Resource;
import javax.naming.NamingException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(urlPatterns = "/test")
public class TestServlet extends HttpServlet {
    @Resource
    private InitialContext initialContext;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        TestEjb ejb = lookup();
        try(PrintWriter writer = resp.getWriter()) {
            if (ejb != null) {
                writer.write(ejb.hello());
                resp.setStatus(200);
            } else {
                resp.setStatus(500);
            }
        }
    }

    private TestEjb lookup() {
        try {
            Object lookup = InitialContext.doLookup("java:app/web/TestEjb!org.wildfly.test.integration.web.annotationsmodule.TestEjb");
            return (TestEjb) lookup;
        } catch (NamingException e) {
            e.printStackTrace();
        }
        return null;
    }
}
