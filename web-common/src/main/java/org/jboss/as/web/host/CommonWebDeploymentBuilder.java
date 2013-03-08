package org.jboss.as.web.host;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Stuart Douglas
 */
public class CommonWebDeploymentBuilder {

    private ClassLoader classLoader;
    private String contextRoot;
    private File documentRoot;
    private final List<CommonServletBuilder> servlets = new ArrayList<>();

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public void setClassLoader(final ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public void addServlet(final CommonServletBuilder servlet) {
        servlets.add(servlet);
    }

    public List<CommonServletBuilder> getServlets() {
        return servlets;
    }

    public String getContextRoot() {
        return contextRoot;
    }

    public void setContextRoot(final String contextRoot) {
        this.contextRoot = contextRoot;
    }

    public File getDocumentRoot() {
        return documentRoot;
    }

    public void setDocumentRoot(final File documentRoot) {
        this.documentRoot = documentRoot;
    }
}
