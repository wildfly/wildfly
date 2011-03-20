/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.deployment.WarMetaData;
import org.jboss.as.weld.WeldDeploymentMarker;
import org.jboss.logging.Logger;
import org.jboss.metadata.web.spec.FilterMappingMetaData;
import org.jboss.metadata.web.spec.FilterMetaData;
import org.jboss.metadata.web.spec.FiltersMetaData;
import org.jboss.metadata.web.spec.ListenerMetaData;
import org.jboss.metadata.web.spec.WebMetaData;

/**
 * Deployment processor that integrates weld into the web tier
 *
 * @author Stuart Douglas
 */
public class WebIntegrationProcessor implements DeploymentUnitProcessor {
    private final ListenerMetaData WBL;
    private final ListenerMetaData JIL;
    private final FilterMetaData CPF;
    private final FilterMappingMetaData CPFM;

    private static final Logger log = Logger.getLogger("org.jboss.as.weld");

    public WebIntegrationProcessor() {

        // create wbl listener
        WBL = new ListenerMetaData();
        WBL.setListenerClass("org.jboss.weld.servlet.WeldListener");
        JIL = new ListenerMetaData();
        JIL.setListenerClass("org.jboss.as.weld.webtier.jsp.JspInitializationListener");
        CPF = new FilterMetaData();
        CPF.setFilterName("Weld Conversation Propagation Filter");
        CPF.setFilterClass("org.jboss.weld.servlet.ConversationPropagationFilter");
        CPFM = new FilterMappingMetaData();
        CPFM.setFilterName("Weld Conversation Propagation Filter");
        CPFM.setUrlPatterns(Arrays.asList("/*"));
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        if (!DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit)) {
            return; // Skip non web deployments
        }

        if(!WeldDeploymentMarker.isPartOfWeldDeployment(deploymentUnit)) {
            return; // skip non weld deployments
        }

        WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if (warMetaData == null) {
            log.info("Not installing Weld web tier integration as no war metadata found");
            return;
        }
        WebMetaData webMetaData = warMetaData.getWebMetaData();
        if (webMetaData == null) {
            log.info("Not installing Weld web tier integration as no web metadata found");
            return;
        }

        List<ListenerMetaData> listeners = webMetaData.getListeners();
        if (listeners == null) {
            listeners = new ArrayList<ListenerMetaData>();
            webMetaData.setListeners(listeners);
        }
        listeners.add(0, WBL);
        listeners.add(1, JIL);

        FiltersMetaData filters = webMetaData.getFilters();
        if (filters == null) {
            filters = new FiltersMetaData();
            webMetaData.setFilters(filters);
        }
        filters.add(CPF);

        List<FilterMappingMetaData> filterMappings = webMetaData.getFilterMappings();
        if (filterMappings == null) {
            filterMappings = new ArrayList<FilterMappingMetaData>();
            webMetaData.setFilterMappings(filterMappings);
        }
        filterMappings.add(CPFM);
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}