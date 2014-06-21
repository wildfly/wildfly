package org.jboss.as.test.integration.web.multipart.defaultservlet;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Stuart Douglas
 */
@WebFilter( urlPatterns = "/*")
public class MultipartFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        Part part = req.getPart("file");
        byte[] data = new byte[100];
        int c;
        InputStream inputStream = part.getInputStream();
        while ((c = inputStream.read(data)) > 0) {
            resp.getOutputStream().write(data, 0, c);
        }
    }

    @Override
    public void destroy() {
    }
}
