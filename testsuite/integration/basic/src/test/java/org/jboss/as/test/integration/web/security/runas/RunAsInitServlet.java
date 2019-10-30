package org.jboss.as.test.integration.web.security.runas;

import java.io.IOException;
import javax.annotation.security.RunAs;
import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Stuart Douglas
 */
@WebServlet(name = "RunAsInitServlet", urlPatterns = "/runAsInit", loadOnStartup = 100)
@RunAs("anil")
public class RunAsInitServlet extends HttpServlet {

    private String initName;

    @EJB
    private CurrentUserEjb currentUserEjb;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().write(initName);
    }

    @Override
    public void init() throws ServletException {
        initName = currentUserEjb.getCurrentUser();
    }
}
