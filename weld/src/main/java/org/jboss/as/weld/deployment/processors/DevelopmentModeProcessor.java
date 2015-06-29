/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
package org.jboss.as.weld.deployment.processors;

import static org.jboss.as.weld.util.Reflections.loadClass;

import java.util.ArrayList;
import java.util.Collections;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.ee.weld.WeldDeploymentMarker;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.as.weld.deployment.WeldPortableExtensions;
import org.jboss.as.weld.util.Reflections;
import org.jboss.as.weld.util.Utils;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.DispatcherType;
import org.jboss.metadata.web.spec.FilterMappingMetaData;
import org.jboss.metadata.web.spec.FilterMetaData;
import org.jboss.metadata.web.spec.FiltersMetaData;
import org.jboss.modules.Module;

/**
 * Deployment processor that initializes Weld Probe if the development mode has been enabled.
 *
 * @author Jozef Hartinger
 *
 */
public class DevelopmentModeProcessor implements DeploymentUnitProcessor {

    private static final String CONTEXT_PARAM_DEV_MODE = "org.jboss.weld.development";
    private static final String PROBE_FILTER_NAME = "weld-probe-filter";
    private static final String PROBE_FILTER_CLASS_NAME = "org.jboss.weld.probe.ProbeFilter";
    private static final String PROBE_EXTENSION_CLASS_NAME = "org.jboss.weld.probe.ProbeExtension";
    private static final FilterMetaData PROBE_FILTER;
    private static final FilterMappingMetaData PROBE_FILTER_MAPPING;

    static {
        PROBE_FILTER = new FilterMetaData();
        PROBE_FILTER.setName(PROBE_FILTER_NAME);
        PROBE_FILTER.setFilterClass(PROBE_FILTER_CLASS_NAME);

        PROBE_FILTER_MAPPING = new FilterMappingMetaData();
        PROBE_FILTER_MAPPING.setFilterName(PROBE_FILTER_NAME);
        PROBE_FILTER_MAPPING.setUrlPatterns(Collections.singletonList("/*"));
        PROBE_FILTER_MAPPING.setDispatchers(Collections.singletonList(DispatcherType.REQUEST));
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);

        if (!DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit)) {
            return; // Skip non web deployments
        }
        if (!WeldDeploymentMarker.isWeldDeployment(deploymentUnit)) {
            return; // skip non weld deployments
        }

        if (deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY) == null) {
            return;
        }
        final JBossWebMetaData webMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY).getMergedJBossWebMetaData();
        if (webMetaData == null) {
            return;
        }

        if (webMetaData.getContextParams() == null) {
            webMetaData.setContextParams(new ArrayList<ParamValueMetaData>());
        }

        final WeldConfiguration configuration = Utils.getRootDeploymentUnit(deploymentUnit).getAttachment(WeldConfiguration.ATTACHMENT_KEY);
        final boolean devModeContextParam = webMetaData.getContextParams().stream()
                .anyMatch(param -> CONTEXT_PARAM_DEV_MODE.equals(param.getParamName()) && Boolean.valueOf(param.getParamValue()));

        // development mode is available when enabled in web.xml, jboss-all.xml or domain mode
        // and the optional probe module is available
        if (!configuration.isDevelopmentMode() && !devModeContextParam) {
            return;
        }
        if (!Reflections.isAccessible(PROBE_FILTER_CLASS_NAME, module.getClassLoader())) {
            return;
        }

        if (webMetaData.getFilters() == null) {
            webMetaData.setFilters(new FiltersMetaData());
        }
        if (webMetaData.getFilterMappings() == null) {
            webMetaData.setFilterMappings(new ArrayList<FilterMappingMetaData>());
        }

        // probe filter
        webMetaData.getFilters().add(PROBE_FILTER);
        // probe filter mapping
        webMetaData.getFilterMappings().add(0, PROBE_FILTER_MAPPING);
        Utils.registerAsComponent(PROBE_FILTER_CLASS_NAME, deploymentUnit);

        WeldPortableExtensions.getPortableExtensions(deploymentUnit).tryRegisterExtension(loadClass(PROBE_EXTENSION_CLASS_NAME, module.getClassLoader()),
                deploymentUnit);
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

}
