/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016, Red Hat, Inc., and individual contributors as indicated
 * by the @authors tag.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.test.integration.modules;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Martin Simka
 */
@WebServlet(name = "SimpleTestServlet", urlPatterns = "/SimpleTestServlet")
public class SimpleTestServlet extends HttpServlet {
    public static final String ACTION_TEST_MODULE_RESOURCE = "testModuleResource";
    public static final String ACTION_TEST_ABSOLUTE_RESOURCE = "testAbsoluteResource";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        if(action == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        PrintWriter out = resp.getWriter();
        if(action.equals(ACTION_TEST_MODULE_RESOURCE)) {
            out.print(ModuleResource.test());
            return;
        } else if(action.equals(ACTION_TEST_ABSOLUTE_RESOURCE)) {
            out.print(AbsoluteResource.test());
            return;
        }
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }
}
