package org.jboss.as.test.clustering;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Utility servlet that provides information on current members of the cluster.
 *
 * @author Tomas Hofman (thofman@redhat.com)
 */
@WebServlet(urlPatterns = {CurrentTopologyServlet.SERVLET_PATH})
public class CurrentTopologyServlet extends HttpServlet {

    private static final String SERVLET_NAME = "topology";
    public static final String SERVLET_PATH = "/" + SERVLET_NAME;
    private static final String CLUSTER = "cluster";

    @EJB
    CurrentTopology currentTopologyBean;

    public static URI createURI(URL baseURL, String cluster) throws URISyntaxException {
        return URI.create(baseURL.toURI().resolve(SERVLET_NAME).toString()
                + '?' + CLUSTER + '=' + cluster);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String cluster = req.getParameter(CLUSTER);
        Set<String> clusterMembers = this.currentTopologyBean.getClusterMembers(cluster);
        for (String member: clusterMembers) {
            resp.getWriter().write(member);
            resp.getWriter().write(",");
        }
    }
}
