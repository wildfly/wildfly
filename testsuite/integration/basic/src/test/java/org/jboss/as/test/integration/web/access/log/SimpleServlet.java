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
