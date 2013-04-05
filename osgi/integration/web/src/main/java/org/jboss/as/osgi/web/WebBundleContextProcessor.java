/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.osgi.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.jboss.as.osgi.OSGiConstants;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.FilterMappingMetaData;
import org.jboss.metadata.web.spec.FilterMetaData;
import org.jboss.metadata.web.spec.FiltersMetaData;
import org.jboss.osgi.metadata.OSGiMetaData;

/**
 * Provide OSGi meatadata for WAB deployments
 *
 * @author Thomas.Diesler@jboss.com
 * @since 30-Nov-2012
 */
public class WebBundleContextProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        WarMetaData warMetaData = depUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        OSGiMetaData metadata = depUnit.getAttachment(OSGiConstants.OSGI_METADATA_KEY);
        if (warMetaData == null || metadata == null)
            return;

        // For confidentiality reasons, a Web Runtime must not return any static content for paths that start with
        // WEB-INF, OSGI-INF, META-INF, OSGI-OPT
        FilterMetaData filterMetaData = new FilterMetaData();
        filterMetaData.setFilterClass(WebBundleContextFilter.class.getName());
        filterMetaData.setFilterName("Filter forbidden resources");

        FilterMappingMetaData filterMappingMetaData = new FilterMappingMetaData();
        filterMappingMetaData.setFilterName(filterMetaData.getName());
        filterMappingMetaData.setUrlPatterns(Arrays.asList("/OSGI-INF/*", "/OSGI-OPT/*"));

        JBossWebMetaData jbossWebMetaData = warMetaData.getMergedJBossWebMetaData();
        FiltersMetaData filters = jbossWebMetaData.getFilters();
        if (filters == null) {
            filters = new FiltersMetaData();
            jbossWebMetaData.setFilters(filters);
        }
        filters.add(filterMetaData);

        List<FilterMappingMetaData> filterMappings = jbossWebMetaData.getFilterMappings();
        if (filterMappings == null) {
            filterMappings = new ArrayList<FilterMappingMetaData>();
            jbossWebMetaData.setFilterMappings(filterMappings);
        }
        filterMappings.add(filterMappingMetaData);
    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
    }

    public static class WebBundleContextFilter implements Filter {

        @Override
        public void init(FilterConfig config) throws ServletException {
        }

        @Override
        public void destroy() {
        }

        @Override
        public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
            chain.doFilter(req, res);
        }
    }
}
