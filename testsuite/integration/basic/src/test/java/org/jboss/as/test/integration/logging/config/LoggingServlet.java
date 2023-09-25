/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.logging.config;

import java.io.IOException;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@WebServlet("/log")
public class LoggingServlet extends HttpServlet {
    static final String LOGGER_NAME = LoggingServlet.class.getName();
    private static final Logger LOGGER = Logger.getLogger(LOGGER_NAME);

    @Inject
    private LoggerResource loggerResource;

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        final String msg = req.getParameter("msg");
        LOGGER.info(formatMessage(msg));
        loggerResource.log(msg);
        loggerResource.logStatic(msg);
    }

    static String formatMessage(final String msg) {
        return String.format("%s - servlet", msg);
    }
}
