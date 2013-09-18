/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.integration.logging.util;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.FATAL;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.logging.Logger;

/**
 * Simple servlet which logs messages on several log levels.
 * 
 * @see LoggingServlet#MSG_TEMPLATE
 * @see LoggingServlet#LOG_LEVELS
 * @author Petr Křemenský <pkremens@redhat.com>
 */
@WebServlet(LoggingServlet.SERVLET_URL)
public class LoggingServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(LoggingServlet.class);

    public static final String SERVLET_URL = "/Logger";
    public static final String PARAM_PREFIX = "text";

    public static final String MSG_TEMPLATE = "{0}LoggingServlet is logging {1} message";
    public static final Logger.Level[] LOG_LEVELS = { DEBUG, TRACE, INFO, WARN, ERROR, FATAL };

    /**
     * Simply logs message generated from {@link #MSG_TEMPLATE} on all levels from {@link #LOG_LEVELS}.
     * <p>
     * If a parameter {@value #PARAM_PREFIX} is provided in the client request, then the generated message is prefixed with the
     * given value (customized by {@link #getPrefix(String)}).
     * <p>
     * Response is HTTP OK (200).
     * 
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final String prefix = getPrefix(request.getParameter(PARAM_PREFIX));
        for (Logger.Level level : LOG_LEVELS) {
            LOGGER.log(level, MessageFormat.format(MSG_TEMPLATE, prefix, getLevelStr(level)));
        }
        response.setStatus(HttpServletResponse.SC_OK);
    }

    /**
     * Returns given parameter customized as message prefix. If null is given, empty string is returned, otherwise the returned
     * value is param + ": ".
     * 
     * @param param
     * @return not-null message prefix
     */
    public static String getPrefix(final String param) {
        return param == null ? "" : (param + ": ");
    }

    /**
     * Converts JBoss log level to lowercase name.
     * 
     * @param level
     * @return
     */
    public static String getLevelStr(final Logger.Level level) {
        return level.name().toLowerCase(Locale.ENGLISH);
    }

}