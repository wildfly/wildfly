package org.jboss.as.test.integration.naming;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

/**
 * @author Parul Sharma
 */
@WebServlet(name = "LookupProperties", urlPatterns = "/properties")
public class LookupProperties extends HttpServlet {

    @Resource(lookup = "java:global/myAppConfig")
    private Map<String, Object> properties;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Writer writer = resp.getWriter();
        StringBuilder sb = new StringBuilder();
        properties.forEach((key, value) ->
                sb.append(key + "=" + value + " " + value.getClass().getSimpleName() + "\n")
        );
        writer.write(sb.toString());
    }
}