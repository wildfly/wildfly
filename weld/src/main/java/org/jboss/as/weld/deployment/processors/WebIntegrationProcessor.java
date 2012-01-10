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
import java.util.ListIterator;
import java.util.Map;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.deployment.WarMetaData;
import org.jboss.as.web.deployment.WebAttachments;
import org.jboss.as.web.deployment.component.ComponentInstantiator;
import org.jboss.as.web.deployment.component.WebComponentDescription;
import org.jboss.as.web.deployment.component.WebComponentInstantiator;
import org.jboss.as.weld.WeldDeploymentMarker;
import org.jboss.as.weld.WeldLogger;
import org.jboss.as.weld.webtier.jsp.JspInitializationListener;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.FilterMappingMetaData;
import org.jboss.metadata.web.spec.FilterMetaData;
import org.jboss.metadata.web.spec.FiltersMetaData;
import org.jboss.metadata.web.spec.ListenerMetaData;
import org.jboss.weld.servlet.ConversationPropagationFilter;
import org.jboss.weld.servlet.WeldListener;

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

    private static final String WELD_LISTENER = WeldListener.class.getName();

    private static final String JSP_LISTENER = JspInitializationListener.class.getName();

    private static final String CONVERSATION_FILTER = ConversationPropagationFilter.class.getName();

    private static final String WELD_SERVLET_LISTENER = "org.jboss.weld.environment.servlet.Listener";

    public WebIntegrationProcessor() {

        // create wbl listener
        WBL = new ListenerMetaData();
        WBL.setListenerClass(WELD_LISTENER);
        JIL = new ListenerMetaData();
        JIL.setListenerClass(JSP_LISTENER);
        CPF = new FilterMetaData();
        CPF.setFilterName("Weld Conversation Propagation Filter");
        CPF.setFilterClass(CONVERSATION_FILTER);
        CPF.setAsyncSupported(true);
        CPFM = new FilterMappingMetaData();
        CPFM.setFilterName("Weld Conversation Propagation Filter");
        CPFM.setUrlPatterns(Arrays.asList("/*"));
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription module = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final EEApplicationClasses applicationClasses = deploymentUnit.getAttachment(Attachments.EE_APPLICATION_CLASSES_DESCRIPTION);

        if (!DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit)) {
            return; // Skip non web deployments
        }

        if (!WeldDeploymentMarker.isPartOfWeldDeployment(deploymentUnit)) {
            return; // skip non weld deployments
        }

        WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if (warMetaData == null) {
            WeldLogger.DEPLOYMENT_LOGGER.debug("Not installing Weld web tier integration as no war metadata found");
            return;
        }
        JBossWebMetaData webMetaData = warMetaData.getMergedJBossWebMetaData();
        if (webMetaData == null) {
            WeldLogger.DEPLOYMENT_LOGGER.debug("Not installing Weld web tier integration as no merged web metadata found");
            return;
        }

        List<ListenerMetaData> listeners = webMetaData.getListeners();
        if (listeners == null) {
            listeners = new ArrayList<ListenerMetaData>();
            webMetaData.setListeners(listeners);
        } else {
            //if the weld servlet listener is present remove it
            //this should allow wars to be portable between AS7 and servlet containers
            final ListIterator<ListenerMetaData> iterator = listeners.listIterator();
            while (iterator.hasNext()) {
                final ListenerMetaData listener = iterator.next();
                if (listener.getListenerClass().trim().equals(WELD_SERVLET_LISTENER)) {
                    WeldLogger.DEPLOYMENT_LOGGER.debugf("Removing weld servlet listener %s from web config, as it is not needed in EE6 environments", WELD_SERVLET_LISTENER);
                    iterator.remove();
                    break;
                }
            }
        }
        listeners.add(0, WBL);
        listeners.add(1, JIL);

        //This uses resource injection, so it needs to be a component
        final WebComponentDescription componentDescription = new WebComponentDescription(JSP_LISTENER, JSP_LISTENER, module, deploymentUnit.getServiceName(), applicationClasses);
        module.addComponent(componentDescription);
        final Map<String, ComponentInstantiator> instantiators = deploymentUnit.getAttachment(WebAttachments.WEB_COMPONENT_INSTANTIATORS);
        instantiators.put(JSP_LISTENER, new WebComponentInstantiator(deploymentUnit, componentDescription));

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