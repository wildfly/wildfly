package org.jboss.as.test.integration.naming.remote.multiple;

import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

@WebServlet(name = "BindRmiServlet", urlPatterns = {"/BindRmiServlet"}, loadOnStartup = 1)
public class BindRmiServlet extends HttpServlet {
    public void init() throws ServletException {
        try {
            InitialContext ctx = new InitialContext();
            ctx.bind("java:jboss/exported/loc/stub", new MyObject());
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
