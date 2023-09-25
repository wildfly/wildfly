/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.json;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.stream.JsonCollectors;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

/**
 * JSON-P 1.1 sample servlet - using JsonCollectors.toJsonArray()
 *
 * @author Rostislav Svoboda
 */
@WebServlet("/json")
public class JSONPServlet extends HttpServlet {

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");

        JsonArray array = this.list.
                stream().
                collect(JsonCollectors.toJsonArray());

        PrintWriter out = response.getWriter();
        out.print(array);
        out.flush();
    }
    private List<JsonObject> list;

    @Override
    public void init() throws ServletException {
        this.list = Arrays.asList(next(), next());
    }

    private JsonObject next() {
        return Json.createObjectBuilder().
                add("number", System.currentTimeMillis()).
                build();
    }
}
