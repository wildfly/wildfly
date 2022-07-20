package org.jboss.as.test.integration.web.multipart.defaultservlet;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
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
