package org.jboss.as.web.host;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.undertow.predicate.Predicate;

/**
 * @author Stuart Douglas
 */
public class WebDeploymentBuilder {

    private ClassLoader classLoader;
    private String contextRoot;
    private File documentRoot;
    private ApplicationContextWrapper applicationContextWrapper;
    private final List<ServletBuilder> servlets = new ArrayList<>();
    private final Map<String,String> mimeTypes = new HashMap<>();
    public final List<Predicate> allowRequestPredicates = new ArrayList<>();

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public WebDeploymentBuilder setClassLoader(final ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    public void addServlet(final ServletBuilder servlet) {
        servlets.add(servlet);
    }

    public List<ServletBuilder> getServlets() {
        return Collections.unmodifiableList(servlets);
    }

    public String getContextRoot() {
        return contextRoot;
    }

    public WebDeploymentBuilder setContextRoot(final String contextRoot) {
        this.contextRoot = contextRoot;
        return this;
    }

    public File getDocumentRoot() {
        return documentRoot;
    }

    public WebDeploymentBuilder setDocumentRoot(final File documentRoot) {
        this.documentRoot = documentRoot;
        return this;
    }

    public WebDeploymentBuilder addAllowedRequestPredicate(Predicate predicate) {
        allowRequestPredicates.add(predicate);
        return this;
    }

    public List<Predicate> getAllowRequestPredicates() {
        return Collections.unmodifiableList(allowRequestPredicates);
    }

    public Map<String, String> getMimeTypes() {
        return mimeTypes;
    }
    public void addMimeMapping(String type,String mapping){
        mimeTypes.put(type,mapping);
    }

    public ApplicationContextWrapper getApplicationContextWrapper() {
        return applicationContextWrapper;
    }

    public void setApplicationContextWrapper(final ApplicationContextWrapper applicationContextWrapper) {
        this.applicationContextWrapper = applicationContextWrapper;
    }
}
