/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.web.access.log;

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.json.Json;
import jakarta.json.stream.JsonGenerator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.MediaType;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@WebServlet("simple")
public class SimpleServlet extends HttpServlet {

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        final PrintWriter writer = response.getWriter();
        response.setContentType(MediaType.APPLICATION_JSON);
        try (JsonGenerator generator = Json.createGenerator(writer)) {
            generator.writeStartObject();

            generator.writeStartObject("parameters");

            request.getParameterMap().forEach((key, values) -> {
                if (values == null) {
                    generator.writeNull(key);
                } else if (values.length > 1) {
                    generator.writeStartArray(key);
                    for (String value : values) {
                        generator.write(value);
                    }
                    generator.writeEnd();
                } else {
                    final String value = values[0];
                    if (value == null) {
                        generator.writeNull(key);
                    } else {
                        generator.write(key, value);
                    }
                }
            });

            generator.writeEnd(); // end parameters

            generator.writeEnd(); // end main
        }
    }
}
