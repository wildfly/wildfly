package org.jboss.as.arquillian.container.remote.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/" + Servlet1.PATTERN)
public class Servlet1 extends HttpServlet {

    public static final String PATTERN = "Servlet1";
            
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException 
    {
        resp.getWriter().append(this.getClass().getName());
    }
}
