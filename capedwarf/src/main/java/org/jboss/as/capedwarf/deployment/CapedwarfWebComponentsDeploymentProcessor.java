package org.jboss.as.capedwarf.deployment;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.metadata.javaee.spec.EnvironmentRefsGroupMetaData;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.javaee.spec.ResourceReferenceMetaData;
import org.jboss.metadata.javaee.spec.ResourceReferencesMetaData;
import org.jboss.metadata.web.spec.DispatcherType;
import org.jboss.metadata.web.spec.FilterMappingMetaData;
import org.jboss.metadata.web.spec.FilterMetaData;
import org.jboss.metadata.web.spec.FiltersMetaData;
import org.jboss.metadata.web.spec.ListenerMetaData;
import org.jboss.metadata.web.spec.ServletMappingMetaData;
import org.jboss.metadata.web.spec.ServletMetaData;
import org.jboss.metadata.web.spec.ServletsMetaData;
import org.jboss.metadata.web.spec.WebMetaData;

import java.util.ArrayList;
import java.util.Arrays;
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
    private static final String ADMIN_SERVLET_NAME = "CapedwarfAdminServlet";

    private final ListenerMetaData GAE_LISTENER;
    private final ListenerMetaData CDI_LISTENER;
    private final ListenerMetaData CDAS_LISTENER;
    private final FilterMetaData GAE_FILTER;
    private final FilterMappingMetaData GAE_FILTER_MAPPING;
    private final ServletMetaData GAE_SERVLET;
    private final ServletMetaData ADMIN_SERVLET;
    private final ServletMappingMetaData GAE_SERVLET_MAPPING;
    private final ServletMappingMetaData ADMIN_SERVLET_MAPPING;
    private final ResourceReferenceMetaData INFINISPAN_REF;

    public CapedwarfWebComponentsDeploymentProcessor() {
        GAE_LISTENER = createGaeListener();
        CDI_LISTENER = createCdiListener();
        CDAS_LISTENER = createAsListener();
        GAE_FILTER = createGaeFilter();
        GAE_FILTER_MAPPING = createGaeFilterMapping();
        GAE_SERVLET = createAuthServlet();
        GAE_SERVLET_MAPPING = createAuthServletMapping();
        ADMIN_SERVLET = createAdminServlet();
        ADMIN_SERVLET_MAPPING = createAdminServletMapping();
        INFINISPAN_REF = createInfinispanRef();
    }

    @Override
    protected void doDeploy(DeploymentUnit unit, WebMetaData webMetaData, Type type) {
        if (type == Type.SPEC) {
            addGaeListenerTo(webMetaData);
            addCdiListenerTo(webMetaData);
            addAsListenerTo(webMetaData);

            addGaeFilterTo(webMetaData);
            addGaeFilterMappingTo(webMetaData);

            addAuthServletTo(webMetaData);
            addAuthServletMappingTo(webMetaData);

            addAdminServletTo(webMetaData);
            addAdminServletMappingTo(webMetaData);

            addResourceReference(webMetaData);
        }
    }

    protected void addContextParamsTo(WebMetaData webMetaData, ParamValueMetaData param) {
        List<ParamValueMetaData> contextParams = webMetaData.getContextParams();
        if (contextParams == null) {
            contextParams = new ArrayList<ParamValueMetaData>();
            webMetaData.setContextParams(contextParams);
        }
        contextParams.add(param);
    }

    private void addGaeListenerTo(WebMetaData webMetaData) {
        getListeners(webMetaData).add(0, GAE_LISTENER);
    }

    private void addCdiListenerTo(WebMetaData webMetaData) {
        getListeners(webMetaData).add(CDI_LISTENER);
    }

    private void addAsListenerTo(WebMetaData webMetaData) {
        getListeners(webMetaData).add(CDAS_LISTENER);
    }

    private ListenerMetaData createCdiListener() {
        ListenerMetaData listener = new ListenerMetaData();
        listener.setListenerClass("org.jboss.capedwarf.appidentity.CDIListener");
        return listener;
    }

    private ListenerMetaData createGaeListener() {
        ListenerMetaData listener = new ListenerMetaData();
        listener.setListenerClass("org.jboss.capedwarf.appidentity.GAEListener");
        return listener;
    }

    private ListenerMetaData createAsListener() {
        ListenerMetaData listener = new ListenerMetaData();
        listener.setListenerClass("org.jboss.as.capedwarf.api.ServletExecutorListener");
        return listener;
    }

    private List<ListenerMetaData> getListeners(WebMetaData webMetaData) {
        List<ListenerMetaData> listeners = webMetaData.getListeners();
        if (listeners == null) {
            listeners = new ArrayList<ListenerMetaData>();
            webMetaData.setListeners(listeners);
        }
        return listeners;
    }

    private void addGaeFilterTo(WebMetaData webMetaData) {
        getFilters(webMetaData).add(GAE_FILTER);
    }

    private FilterMetaData createGaeFilter() {
        FilterMetaData filter = new FilterMetaData();
        filter.setFilterName(GAE_FILTER_NAME);
        filter.setFilterClass("org.jboss.capedwarf.appidentity.GAEFilter");
        return filter;
    }

    private FiltersMetaData getFilters(WebMetaData webMetaData) {
        FiltersMetaData filters = webMetaData.getFilters();
        if (filters == null) {
            filters = new FiltersMetaData();
            webMetaData.setFilters(filters);
        }
        return filters;
    }

    private void addGaeFilterMappingTo(WebMetaData webMetaData) {
        getFilterMappings(webMetaData).add(0, GAE_FILTER_MAPPING);
    }

    private FilterMappingMetaData createGaeFilterMapping() {
        FilterMappingMetaData filterMapping = new FilterMappingMetaData();
        filterMapping.setFilterName(GAE_FILTER_NAME);
        filterMapping.setUrlPatterns(Collections.singletonList("/*"));
        filterMapping.setDispatchers(Arrays.asList(DispatcherType.REQUEST, DispatcherType.FORWARD));
        return filterMapping;
    }

    private List<FilterMappingMetaData> getFilterMappings(WebMetaData webMetaData) {
        List<FilterMappingMetaData> filterMappings = webMetaData.getFilterMappings();
        if (filterMappings == null) {
            filterMappings = new ArrayList<FilterMappingMetaData>();
            webMetaData.setFilterMappings(filterMappings);
        }
        return filterMappings;
    }

    private void addAuthServletTo(WebMetaData webMetaData) {
        getServlets(webMetaData).add(GAE_SERVLET);
    }

    private void addAdminServletTo(WebMetaData webMetaData) {
        getServlets(webMetaData).add(ADMIN_SERVLET);
    }

    private ServletsMetaData getServlets(WebMetaData webMetaData) {
        ServletsMetaData servletsMetaData = webMetaData.getServlets();
        if (servletsMetaData == null) {
            servletsMetaData = new ServletsMetaData();
            webMetaData.setServlets(servletsMetaData);
        }
        return servletsMetaData;
    }

    private ServletMetaData createAuthServlet() {
        ServletMetaData servlet = new ServletMetaData();
        servlet.setServletName(AUTH_SERVLET_NAME);
        servlet.setServletClass("org.jboss.capedwarf.users.AuthServlet");
        servlet.setEnabled(true);
        return servlet;
    }

    private ServletMetaData createAdminServlet() {
        ServletMetaData servlet = new ServletMetaData();
        servlet.setServletName(ADMIN_SERVLET_NAME);
        servlet.setServletClass("org.jboss.capedwarf.admin.AdminServlet");
        servlet.setEnabled(true);
        return servlet;
    }

    private void addAuthServletMappingTo(WebMetaData webMetaData) {
        getServletMappings(webMetaData).add(GAE_SERVLET_MAPPING);
    }

    private void addAdminServletMappingTo(WebMetaData webMetaData) {
        getServletMappings(webMetaData).add(ADMIN_SERVLET_MAPPING);
    }

    private List<ServletMappingMetaData> getServletMappings(WebMetaData webMetaData) {
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
        servletMapping.setUrlPatterns(Collections.singletonList("/_capedwarf_/auth/*"));   // TODO: introduce AuthServlet.URL_PATTERN
        return servletMapping;
    }

    private ServletMappingMetaData createAdminServletMapping() {
        ServletMappingMetaData servletMapping = new ServletMappingMetaData();
        servletMapping.setServletName(ADMIN_SERVLET_NAME);
        servletMapping.setUrlPatterns(Collections.singletonList("/_ah/admin/*"));
        return servletMapping;
    }

    private ResourceReferenceMetaData createInfinispanRef() {
        ResourceReferenceMetaData ref = new ResourceReferenceMetaData();
        ref.setResourceRefName("infinispan/container/capedwarf");
        ref.setJndiName("java:jboss/infinispan/container/capedwarf");
        ref.setType("org.infinispan.manager.EmbeddedCacheManager");
        return ref;
    }

    private void addResourceReference(WebMetaData webMetaData) {
        ResourceReferencesMetaData references = webMetaData.getResourceReferences();
        if (references == null) {
            references = new ResourceReferencesMetaData();
            EnvironmentRefsGroupMetaData env = webMetaData.getJndiEnvironmentRefsGroup();
            if (env == null) {
                env = new EnvironmentRefsGroupMetaData();
                webMetaData.setJndiEnvironmentRefsGroup(env);
            }
            env.setResourceReferences(references);
        }
        references.add(INFINISPAN_REF);
    }
}
