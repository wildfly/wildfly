package org.jboss.as.test.integration.ejb.access.log;

import org.jboss.logging.Logger;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "SLSBLocalServlet", urlPatterns = {"/slsblocal"})
public class SLSBLocalServlet extends HttpServlet {

    protected static final Logger logger = Logger.getLogger(SLSBLocalServlet.class);

    @EJB
    SLSBLocal slsb;

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String echo = slsb.echo("TUTTO A POSTO A FERRAGOSTO");
        if (echo != null) {
            resp.getWriter().write(echo);
        }
        logger.info("CallSLSBLocalServlet: " + echo);
    }
}
