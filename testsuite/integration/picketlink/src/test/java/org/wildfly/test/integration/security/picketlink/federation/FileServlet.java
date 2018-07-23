package org.wildfly.test.integration.security.picketlink.federation;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.AbstractMap;
import java.util.Optional;

@WebServlet("/upload")
@MultipartConfig
public class FileServlet extends HttpServlet {
    protected void processRequest(HttpServletRequest request,
                                  HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");


        try (PrintWriter writer = response.getWriter()) {
            Optional<Part> part = Optional.ofNullable(request.getPart("test-data"));
            part.map(p->{
                try {
                    String name = p.getName();
                    String value = convertStreamToString(p.getInputStream());
                    return new AbstractMap.SimpleEntry<String, String>(name, value);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            })
                    .filter(m->m!=null)
                    .ifPresent(m->writer.println("Test Data: " + m.getValue()));

        }


    }

    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        processRequest(req, resp);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        processRequest(req, resp);
    }
}