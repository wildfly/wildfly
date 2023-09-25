/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.json;

import jakarta.json.bind.JsonbBuilder;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * JSON-B 1.0 sample servlet - using JsonbBuilder
 *
 * @author Rostislav Svoboda
 */
@WebServlet("/jsonb")
public class JSONBServlet extends HttpServlet {

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        out.print(JsonbBuilder.create().toJson(new Person("foo", "bar")));
        out.flush();
    }

    public static class Person {
        public String name;
        public String surname;

        public Person(String name, String surname) {
            this.name = name;
            this.surname = surname;
        }
    }
}
