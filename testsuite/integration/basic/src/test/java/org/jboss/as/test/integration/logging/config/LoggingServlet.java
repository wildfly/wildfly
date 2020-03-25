/*
 * Copyright 2020 Red Hat, Inc.
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

package org.jboss.as.test.integration.logging.config;

import java.io.IOException;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
