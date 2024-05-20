package org.jboss.as.test.clustering.single.web.shared;

import jakarta.ejb.EJB;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/count")
public class CounterServlet extends HttpServlet {

    @EJB
    SessionDestroyCounter sessionDestroyCounter;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain");
        int sessionDestroyCount = sessionDestroyCounter.getSessionDestroyCount();
        PrintWriter printWriter = resp.getWriter();
        printWriter.print(sessionDestroyCount);
    }
}

