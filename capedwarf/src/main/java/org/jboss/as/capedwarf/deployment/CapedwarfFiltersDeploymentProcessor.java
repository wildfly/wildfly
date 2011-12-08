package org.jboss.as.capedwarf.deployment;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.deployment.WarMetaData;
import org.jboss.capedwarf.appidentity.GAEFilter;
import org.jboss.capedwarf.users.AuthServlet;
import org.jboss.logging.Logger;
import org.jboss.metadata.web.spec.FilterMappingMetaData;
import org.jboss.metadata.web.spec.FilterMetaData;
import org.jboss.metadata.web.spec.FiltersMetaData;
import org.jboss.metadata.web.spec.ServletMappingMetaData;
import org.jboss.metadata.web.spec.ServletMetaData;
import org.jboss.metadata.web.spec.ServletsMetaData;
import org.jboss.metadata.web.spec.WebMetaData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:marko.luksa@gmail.com">Marko Luksa</a>
 */
public class CapedwarfFiltersDeploymentProcessor implements DeploymentUnitProcessor {

    Logger log = Logger.getLogger(CapedwarfFiltersDeploymentProcessor.class);


    /**
     * The relative order of this processor within the {@link #PHASE}.
     * The current number is large enough for it to happen after all
     * the standard deployment unit processors that come with JBoss AS.
     */
    public static final int PRIORITY = 0x3B00;

    private static final String GAE_FILTER_NAME = "GAEFilter";
    private static final String AUTH_SERVLET_NAME = "authservlet";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        WebMetaData webMetaData = getWebMetaData(phaseContext);
        if (webMetaData == null) {
            return;
        }

        addFilterTo(webMetaData);
        addFilterMappingTo(webMetaData);

        addAuthServletTo(webMetaData);
        addAuthServletMappingTo(webMetaData);
    }

    private WebMetaData getWebMetaData(DeploymentPhaseContext phaseContext) {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        return warMetaData == null ? null : warMetaData.getSharedWebMetaData();
    }

    private void addFilterTo(WebMetaData webMetaData) {
        getFilters(webMetaData).add(createFilter());
    }

    private FilterMetaData createFilter() {
        FilterMetaData filter = new FilterMetaData();
        filter.setFilterName(GAE_FILTER_NAME);
        filter.setFilterClass(GAEFilter.class.getName());
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

    private void addFilterMappingTo(WebMetaData webMetaData) {
        getFilterMappings(webMetaData).add(createFilterMapping());
    }

    private FilterMappingMetaData createFilterMapping() {
        FilterMappingMetaData filterMapping = new FilterMappingMetaData();
        filterMapping.setFilterName(GAE_FILTER_NAME);
        filterMapping.setUrlPatterns(Collections.singletonList("/*"));
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
        getServlets(webMetaData).add(createAuthServlet());
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
        servlet.setServletClass(AuthServlet.class.getName());
        servlet.setEnabled(true);
        return servlet;
    }

    private void addAuthServletMappingTo(WebMetaData webMetaData) {
        getServletMappings(webMetaData).add(createAuthServletMapping());
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
        servletMapping.setUrlPatterns(Collections.singletonList(AuthServlet.SERVLET_URI + "/*"));   // TODO: introduce AuthServlet.URL_PATTERN
        return servletMapping;
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}
