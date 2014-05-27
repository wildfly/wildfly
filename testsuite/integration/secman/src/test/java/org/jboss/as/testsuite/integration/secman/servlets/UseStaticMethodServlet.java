package org.jboss.as.testsuite.integration.secman.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.as.testsuite.integration.secman.PropertyReadStaticMethodClass;

@WebServlet(UseStaticMethodServlet.SERVLET_PATH)
public class UseStaticMethodServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public static final String SERVLET_PATH = "/UseStaticMethod";
    public static final String PARAM_PROPERTY_NAME = "property";
    public static final String DEFAULT_PROPERTY_NAME = "java.home";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        String property = req.getParameter(PARAM_PROPERTY_NAME);
        if (property == null || property.length() == 0) {
            property = DEFAULT_PROPERTY_NAME;
        }
        final PrintWriter writer = resp.getWriter();
        writer.write(PropertyReadStaticMethodClass.readProperty(property));
        writer.close();
    }
}

