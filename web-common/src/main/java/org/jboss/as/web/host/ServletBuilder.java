package org.jboss.as.web.host;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;

/**
 * @author Stuart Douglas
 */
public class ServletBuilder {

    private Class<?> servletClass;
    private Servlet servlet;
    private String servletName;
    private boolean forceInit;
    private final List<String> urlMappings = new ArrayList<>();
    private final Map<String, String> initParams = new LinkedHashMap<>();

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

    public ServletBuilder addUrlMapping(final String mapping) {
        this.urlMappings.add(mapping);
        return this;
    }

    public ServletBuilder addUrlMappings(final String... mappings) {
        this.urlMappings.addAll(Arrays.asList(mappings));
        return this;
    }

    public ServletBuilder addUrlMappings(final Collection<String> mappings) {
        this.urlMappings.addAll(mappings);
        return this;
    }

    public List<String> getUrlMappings() {
        return Collections.unmodifiableList(urlMappings);
    }

    public ServletBuilder addInitParam(final String name, final String value) {
        initParams.put(name, value);
        return this;
    }

    public Map<String, String> getInitParams() {
        return Collections.unmodifiableMap(initParams);
    }
    public boolean isForceInit() {
          return forceInit;
      }

      public void setForceInit(boolean forceInit) {
          this.forceInit = forceInit;
      }
}
