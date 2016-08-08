package org.jboss.as.test.integration.logging.profiles;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.logging.Logger;

/**
 * 
 * @author Petr Kremensky <pkremens@redhat.com>
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
