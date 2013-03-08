package org.jboss.as.web.host;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.Servlet;

/**
 * @author Stuart Douglas
 */
public class CommonServletBuilder {

    private Class<?> servletClass;
    private Servlet servlet;
    private String servletName;

    private final Set<String> urlMappings = new HashSet<>();
    private final Map<String, String> initParams = new HashMap<>();

    public Class<?> getServletClass() {
        return servletClass;
    }

    public void setServletClass(final Class<?> servletClass) {
        this.servletClass = servletClass;
    }

    public Servlet getServlet() {
        return servlet;
    }

    public void setServlet(final Servlet servlet) {
        this.servlet = servlet;
    }

    public String getServletName() {
        return servletName;
    }

    public void setServletName(final String servletName) {
        this.servletName = servletName;
    }

    public Set<String> getUrlMappings() {
        return urlMappings;
    }

    public Map<String, String> getInitParams() {
        return initParams;
    }
}
