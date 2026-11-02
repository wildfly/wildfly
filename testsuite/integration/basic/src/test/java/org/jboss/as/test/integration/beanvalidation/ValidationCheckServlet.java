/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.beanvalidation;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

/**
 * Servlet that validates a {@link ValidationXmlEarBean} using the <em>CDI-injected</em>
 * {@link Validator} and returns the violation count and messages as plain text.
 * <p>
 * The injected {@code Validator} is backed by the deployment-wide, application-scoped
 * {@code ValidatorFactory}. This servlet exercises, from the web tier, that the factory was built
 * with the WAR's class loader so the WAR's {@code validation.xml} is honoured (WFLY-21093). A first
 * line of {@code 0} means {@code validation.xml} was not applied.
 * <p>
 * Response format:
 * <pre>
 * &lt;violation-count&gt;
 * &lt;property-path&gt;=&lt;message&gt;
 * ...
 * </pre>
 */
@WebServlet("/validate")
public class ValidationCheckServlet extends HttpServlet {

    @Inject
    private Validator validator;

    /**
     * Validates a bean constrained solely through {@code validation.xml} and writes the violation
     * count (first line) followed by one {@code path=message} line per violation.
     *
     * @param req  the request (unused)
     * @param resp the response receiving the plain-text report
     * @throws IOException if writing the response fails
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Set<ConstraintViolation<ValidationXmlEarBean>> violations = validator.validate(new ValidationXmlEarBean());

        resp.setContentType("text/plain");
        PrintWriter writer = resp.getWriter();
        writer.println(violations.size());
        for (ConstraintViolation<ValidationXmlEarBean> violation : violations) {
            writer.println(violation.getPropertyPath() + "=" + violation.getMessage());
        }
    }
}
