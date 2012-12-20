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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.logging.Logger;

/**
 * 
 * @author Petr Křemenský <pkremens@redhat.com>
 */
@WebServlet("/Logger")
public class LoggingServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(LoggingServlet.class);

	public LoggingServlet() {
	}

	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		String parameter = "";
		if (request.getParameter("text") != null) {
			parameter = request.getParameter("text") + ": ";
		}
		logger.debug(parameter + "LoggingServlet is logging debug message");
		logger.trace(parameter + "LoggingServlet is logging trace message");
		logger.info(parameter + "LoggingServlet is logging info message");
		logger.warn(parameter + "LoggingServlet is logging warn message");
		logger.error(parameter + "LoggingServlet is logging error message");
		logger.fatal(parameter + "LoggingServlet is logging fatal message");
		response.setStatus(HttpServletResponse.SC_OK);
	}
}