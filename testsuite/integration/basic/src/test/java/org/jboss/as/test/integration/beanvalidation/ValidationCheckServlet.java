/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.beanvalidation;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

/**
 * Servlet that validates a {@link ValidationXmlEarBean} with a null {@code name} field
 * and returns the violation count and messages as plain text.
 * <p>
 * Uses {@link Validation#buildDefaultValidatorFactory()} so that the thread context
 * classloader (TCCL) determines which {@code validation.xml} is discovered.
 * <p>
 * Response format:
 * <pre>
 * &lt;violation-count&gt;
 * &lt;property-path&gt;=&lt;message-1&gt;
 * &lt;property-path&gt;=&lt;message-2&gt;
 * ...
 * </pre>
 */
@WebServlet("/validate")
public class ValidationCheckServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ValidationXmlEarBean bean = new ValidationXmlEarBean();

        ValidatorFactory vf = Validation.buildDefaultValidatorFactory();
        try {
            Set<ConstraintViolation<ValidationXmlEarBean>> violations = vf.getValidator().validate(bean);

            resp.setContentType("text/plain");
            PrintWriter writer = resp.getWriter();
            writer.println(violations.size());
            for (ConstraintViolation<ValidationXmlEarBean> violation : violations) {
                writer.println(violation.getPropertyPath() + "=" + violation.getMessage());
            }
        } finally {
            vf.close();
        }
    }
}
