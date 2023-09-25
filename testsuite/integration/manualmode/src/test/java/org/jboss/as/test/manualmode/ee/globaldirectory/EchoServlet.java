/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.manualmode.ee.globaldirectory;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Properties;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
