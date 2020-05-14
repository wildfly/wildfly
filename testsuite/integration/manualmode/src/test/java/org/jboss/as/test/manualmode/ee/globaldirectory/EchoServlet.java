/*
 * Copyright 2019 Red Hat, Inc.
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

package org.jboss.as.test.manualmode.ee.globaldirectory;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "EchoServlet", urlPatterns = "/echoServlet")
public class EchoServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request,response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter out = response.getWriter();

        String propertiesName = request.getParameter("prop");

        EchoUtility echoUtil = new EchoUtility();

        out.print(echoUtil.echo("Message from the servlet"));

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        for (String s : Arrays.asList("global-directory.properties", "sub/sub-global-directory.properties")) {
            try (InputStream is = classLoader.getResourceAsStream(s)) {
                if (is == null) {
                    out.println(propertiesName + " not found.");
                } else {
                    Properties prop = new Properties();
                    prop.load(is);
                    prop.forEach((key, value) -> out.print(" Key=" + key + ", Value=" + value));
                }
            } catch (Exception e) {
                throw new ServletException(e);
            }
        }

        out.flush();
        out.close();
    }


}
