package org.jboss.as.capedwarf.deployment;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.capedwarf.admin.CustomResourceResolver;
import org.jboss.capedwarf.appidentity.CDIListener;
import org.jboss.capedwarf.appidentity.GAEFilter;
import org.jboss.capedwarf.users.AuthServlet;
import org.jboss.metadata.javaee.spec.Environment;
import org.jboss.metadata.javaee.spec.EnvironmentRefsGroupMetaData;
import org.jboss.metadata.javaee.spec.MutableRemoteEnvironment;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.javaee.spec.ResourceReferenceMetaData;
import org.jboss.metadata.javaee.spec.ResourceReferencesMetaData;
import org.jboss.metadata.web.jboss.JBossServletMetaData;
import org.jboss.metadata.web.jboss.JBossServletsMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.FilterMappingMetaData;
import org.jboss.metadata.web.spec.FilterMetaData;
import org.jboss.metadata.web.spec.FiltersMetaData;
import org.jboss.metadata.web.spec.ListenerMetaData;
import org.jboss.metadata.web.spec.ServletMappingMetaData;

import javax.faces.view.facelets.ResourceResolver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Add GAE filter and auth servlet.
 * Enable Faces, if not yet configured.
 *
 * @author <a href="mailto:marko.luksa@gmail.com">Marko Luksa</a>
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class CapedwarfWebComponentsDeploymentProcessor extends CapedwarfWebModificationDeploymentProcessor {

    private static final String GAE_FILTER_NAME = "GAEFilter";
    private static final String AUTH_SERVLET_NAME = "authservlet";

    private static final String FACES_SERVLET_CLASS = "javax.faces.webapp.FacesServlet";
    private static final String FACES_SERVLET_NAME = "_FacesServlet";
    private static final String FACES_SERVLET_PATTERN = "*.jsf";
    private static final String FACES_LISTENER_CLASS = "com.sun.faces.config.ConfigureListener";

    private final ListenerMetaData CDI_LISTENER;
    private final FilterMetaData GAE_FILTER;
    private final FilterMappingMetaData GAE_FILTER_MAPPING;
    private final JBossServletMetaData GAE_SERVLET;
    private final ServletMappingMetaData GAE_SERVLET_MAPPING;
    private final JBossServletMetaData FACES_SERVLET;
    private final ListenerMetaData FACES_LISTENER;
    private final ServletMappingMetaData FACES_SERVLET_MAPPING;
    private final ResourceReferenceMetaData INFINISPAN_REF;

    public CapedwarfWebComponentsDeploymentProcessor() {
        CDI_LISTENER = createCdiListener();
        GAE_FILTER = createGaeFilter();
        GAE_FILTER_MAPPING = createGaeFilterMapping();
        GAE_SERVLET = createAuthServlet();
        GAE_SERVLET_MAPPING = createAuthServletMapping();
        FACES_SERVLET = createFacesServlet();
        FACES_LISTENER = createFacesListener();
        FACES_SERVLET_MAPPING = createFacesServletMapping();
        INFINISPAN_REF = createInfinispanRef();
    }

    @Override
    protected void doDeploy(DeploymentUnit unit, JBossWebMetaData webMetaData, Type type) {
        if (type == Type.MERGED) {
            addContextParamsTo(webMetaData);
            addCdiListenerTo(webMetaData);

            addGaeFilterTo(webMetaData);
            addGaeFilterMappingTo(webMetaData);

            addAuthServletTo(webMetaData);
            addAuthServletMappingTo(webMetaData);

            if (addFacesServlet(webMetaData)) {
                addFacesMappingTo(webMetaData);
                // addFacesListenerTo(webMetaData);
            }

            addResourceReference(webMetaData);
        }
    }

    private void addContextParamsTo(JBossWebMetaData webMetaData) {
        ParamValueMetaData param = new ParamValueMetaData();
        param.setParamName(ResourceResolver.FACELETS_RESOURCE_RESOLVER_PARAM_NAME);
        param.setParamValue(CustomResourceResolver.class.getName());

        List<ParamValueMetaData> contextParams = webMetaData.getContextParams();
        if (contextParams == null) {
            contextParams = new ArrayList<ParamValueMetaData>();
            webMetaData.setContextParams(contextParams);
        }
        contextParams.add(param);
    }

    private void addCdiListenerTo(JBossWebMetaData webMetaData) {
        getListeners(webMetaData).add(CDI_LISTENER);
    }

    @SuppressWarnings("UnusedDeclaration")
    private void addFacesListenerTo(JBossWebMetaData webMetaData) {
        getListeners(webMetaData).add(FACES_LISTENER);
    }

    private ListenerMetaData createCdiListener() {
        ListenerMetaData listener = new ListenerMetaData();
        listener.setListenerClass(CDIListener.class.getName());
        return listener;
    }

    private ListenerMetaData createFacesListener() {
        ListenerMetaData listener = new ListenerMetaData();
        listener.setListenerClass(FACES_LISTENER_CLASS);
        return listener;
    }

    private List<ListenerMetaData> getListeners(JBossWebMetaData webMetaData) {
        List<ListenerMetaData> listeners = webMetaData.getListeners();
        if (listeners == null) {
            listeners = new ArrayList<ListenerMetaData>();
            webMetaData.setListeners(listeners);
        }
        return listeners;
    }

    private void addGaeFilterTo(JBossWebMetaData webMetaData) {
        getFilters(webMetaData).add(GAE_FILTER);
    }

    private FilterMetaData createGaeFilter() {
        FilterMetaData filter = new FilterMetaData();
        filter.setFilterName(GAE_FILTER_NAME);
        filter.setFilterClass(GAEFilter.class.getName());
        return filter;
    }

    private FiltersMetaData getFilters(JBossWebMetaData webMetaData) {
        FiltersMetaData filters = webMetaData.getFilters();
        if (filters == null) {
            filters = new FiltersMetaData();
            webMetaData.setFilters(filters);
        }
        return filters;
    }

    private void addGaeFilterMappingTo(JBossWebMetaData webMetaData) {
        getFilterMappings(webMetaData).add(0, GAE_FILTER_MAPPING);
    }

    private FilterMappingMetaData createGaeFilterMapping() {
        FilterMappingMetaData filterMapping = new FilterMappingMetaData();
        filterMapping.setFilterName(GAE_FILTER_NAME);
        filterMapping.setUrlPatterns(Collections.singletonList("/*"));
        return filterMapping;
    }

    private List<FilterMappingMetaData> getFilterMappings(JBossWebMetaData webMetaData) {
        List<FilterMappingMetaData> filterMappings = webMetaData.getFilterMappings();
        if (filterMappings == null) {
            filterMappings = new ArrayList<FilterMappingMetaData>();
            webMetaData.setFilterMappings(filterMappings);
        }
        return filterMappings;
    }

    private void addAuthServletTo(JBossWebMetaData webMetaData) {
        getServlets(webMetaData).add(GAE_SERVLET);
    }

    private boolean addFacesServlet(JBossWebMetaData webMetaData) {
        final JBossServletsMetaData servlets = getServlets(webMetaData);
        for (JBossServletMetaData servlet : servlets) {
            if (FACES_SERVLET_CLASS.equals(servlet.getServletClass()))
                return false;
        }
        servlets.add(FACES_SERVLET);
        return true;
    }

    private JBossServletsMetaData getServlets(JBossWebMetaData webMetaData) {
        JBossServletsMetaData servletsMetaData = webMetaData.getServlets();
        if (servletsMetaData == null) {
            servletsMetaData = new JBossServletsMetaData();
            webMetaData.setServlets(servletsMetaData);
        }
        return servletsMetaData;
    }

    private JBossServletMetaData createAuthServlet() {
        JBossServletMetaData servlet = new JBossServletMetaData();
        servlet.setServletName(AUTH_SERVLET_NAME);
        servlet.setServletClass(AuthServlet.class.getName());
        servlet.setEnabled(true);
        return servlet;
    }

    private JBossServletMetaData createFacesServlet() {
        JBossServletMetaData servlet = new JBossServletMetaData();
        servlet.setServletName(FACES_SERVLET_NAME);
        servlet.setServletClass(FACES_SERVLET_CLASS);
        servlet.setEnabled(true);
        return servlet;
    }

    private void addAuthServletMappingTo(JBossWebMetaData webMetaData) {
        getServletMappings(webMetaData).add(GAE_SERVLET_MAPPING);
    }

    private void addFacesMappingTo(JBossWebMetaData webMetaData) {
        getServletMappings(webMetaData).add(FACES_SERVLET_MAPPING);
    }

    private List<ServletMappingMetaData> getServletMappings(JBossWebMetaData webMetaData) {
        List<ServletMappingMetaData> servletMappings = webMetaData.getServletMappings();
        if (servletMappings == null) {
            servletMappings = new ArrayList<ServletMappingMetaData>();
            webMetaData.setServletMappings(servletMappings);
        }
        return servletMappings;
    }

    private ServletMappingMetaData createAuthServletMapping() {
        ServletMappingMetaData servletMapping = new ServletMappingMetaData();
        servletMapping.setServletName(AUTH_SERVLET_NAME);
        servletMapping.setUrlPatterns(Collections.singletonList(AuthServlet.SERVLET_URI + "/*"));   // TODO: introduce AuthServlet.URL_PATTERN
        return servletMapping;
    }

    private ServletMappingMetaData createFacesServletMapping() {
        ServletMappingMetaData servletMapping = new ServletMappingMetaData();
        servletMapping.setServletName(FACES_SERVLET_NAME);
        servletMapping.setUrlPatterns(Collections.singletonList(FACES_SERVLET_PATTERN));
        return servletMapping;
    }

    private ResourceReferenceMetaData createInfinispanRef() {
        ResourceReferenceMetaData ref = new ResourceReferenceMetaData();
        ref.setResourceRefName("infinispan/container/capedwarf");
        ref.setJndiName("java:jboss/infinispan/container/capedwarf");
        ref.setType("org.infinispan.manager.EmbeddedCacheManager");
        return ref;
    }

    private void addResourceReference(JBossWebMetaData webMetaData) {
        ResourceReferencesMetaData references = webMetaData.getResourceReferences();
        if (references == null) {
            references = new ResourceReferencesMetaData();
            Environment env = webMetaData.getJndiEnvironmentRefsGroup();
            if (env == null) {
                env = new EnvironmentRefsGroupMetaData();
                webMetaData.setJndiEnvironmentRefsGroup(env);
            }
            if (env instanceof MutableRemoteEnvironment) {
                MutableRemoteEnvironment mre = (MutableRemoteEnvironment) env;
                mre.setResourceReferences(references);
            }
        }
        references.add(INFINISPAN_REF);
    }
}
