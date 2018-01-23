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

import static org.jboss.as.weld.util.Utils.registerAsComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.ee.weld.WeldDeploymentMarker;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.common.ExpressionFactoryWrapper;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.as.weld.WeldStartService;
import org.jboss.as.weld.logging.WeldLogger;
import org.jboss.as.weld.webtier.jsp.WeldJspExpressionFactoryWrapper;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.FilterMappingMetaData;
import org.jboss.metadata.web.spec.FilterMetaData;
import org.jboss.metadata.web.spec.FiltersMetaData;
import org.jboss.metadata.web.spec.ListenerMetaData;
import org.jboss.msc.service.ServiceName;
import org.jboss.weld.module.web.servlet.ConversationFilter;
import org.jboss.weld.module.web.servlet.WeldInitialListener;
import org.jboss.weld.module.web.servlet.WeldTerminalListener;
import org.jboss.weld.servlet.api.InitParameters;

/**
 * Deployment processor that integrates weld into the web tier
 *
 * @author Stuart Douglas
 */
public class WebIntegrationProcessor implements DeploymentUnitProcessor {
    private final ListenerMetaData INITIAL_LISTENER_METADATA;
    private final ListenerMetaData TERMINAL_LISTENER_MEDATADA;
    private final FilterMetaData conversationFilterMetadata;

    private static final String WELD_INITIAL_LISTENER = WeldInitialListener.class.getName();
    private static final String WELD_TERMINAL_LISTENER = WeldTerminalListener.class.getName();

    private static final String WELD_SERVLET_LISTENER = "org.jboss.weld.environment.servlet.Listener";

    private static final String CONVERSATION_FILTER_CLASS = ConversationFilter.class.getName();
    private static final String CONVERSATION_FILTER_NAME = "CDI Conversation Filter";

    private static final ParamValueMetaData CONVERSATION_FILTER_INITIALIZED = new ParamValueMetaData();

    public WebIntegrationProcessor() {

        // create wbl listener
        INITIAL_LISTENER_METADATA = new ListenerMetaData();
        INITIAL_LISTENER_METADATA.setListenerClass(WELD_INITIAL_LISTENER);
        TERMINAL_LISTENER_MEDATADA = new ListenerMetaData();
        TERMINAL_LISTENER_MEDATADA.setListenerClass(WELD_TERMINAL_LISTENER);
        conversationFilterMetadata = new FilterMetaData();
        conversationFilterMetadata.setFilterClass(CONVERSATION_FILTER_CLASS);
        conversationFilterMetadata.setFilterName(CONVERSATION_FILTER_NAME);
        conversationFilterMetadata.setAsyncSupported(true);
        CONVERSATION_FILTER_INITIALIZED.setParamName(ConversationFilter.CONVERSATION_FILTER_REGISTERED);
        CONVERSATION_FILTER_INITIALIZED.setParamValue(Boolean.TRUE.toString());
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription module = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final EEApplicationClasses applicationClasses = deploymentUnit.getAttachment(Attachments.EE_APPLICATION_CLASSES_DESCRIPTION);

        if (!DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit)) {
            return; // Skip non web deployments
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
        if(!WeldDeploymentMarker.isWeldDeployment(deploymentUnit)) {
            if (WeldDeploymentMarker.isPartOfWeldDeployment(deploymentUnit)) {
                createDependency(deploymentUnit, warMetaData);
            }
            return;
        }

        createDependency(deploymentUnit, warMetaData);

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
        listeners.add(0, INITIAL_LISTENER_METADATA);
        listeners.add(TERMINAL_LISTENER_MEDATADA);

        //These listeners use resource injection, so they need to be components
        registerAsComponent(WELD_INITIAL_LISTENER, deploymentUnit);
        registerAsComponent(WELD_TERMINAL_LISTENER, deploymentUnit);

        deploymentUnit.addToAttachmentList(ExpressionFactoryWrapper.ATTACHMENT_KEY, WeldJspExpressionFactoryWrapper.INSTANCE);

        if (webMetaData.getContextParams() == null) {
            webMetaData.setContextParams(new ArrayList<ParamValueMetaData>());
        }
        final List<ParamValueMetaData> contextParams = webMetaData.getContextParams();
        setupWeldContextIgnores(contextParams, InitParameters.CONTEXT_IGNORE_FORWARD);
        setupWeldContextIgnores(contextParams, InitParameters.CONTEXT_IGNORE_INCLUDE);

        if (webMetaData.getFilterMappings() != null) {
            // register ConversationFilter
            boolean filterMappingFound = false;
            for (FilterMappingMetaData mapping : webMetaData.getFilterMappings()) {
                if (CONVERSATION_FILTER_NAME.equals(mapping.getFilterName())) {
                    filterMappingFound = true;
                    break;
                }
            }

            if (filterMappingFound) { // otherwise WeldListener will take care of conversation context activation
                boolean filterFound = false;
                // register ConversationFilter
                if (webMetaData.getFilters() == null) {
                    webMetaData.setFilters(new FiltersMetaData());
                }
                for (FilterMetaData filter : webMetaData.getFilters()) {
                    if (CONVERSATION_FILTER_CLASS.equals(filter.getFilterClass())) {
                        filterFound = true;
                        break;
                    }
                }
                if (!filterFound) {
                    webMetaData.getFilters().add(conversationFilterMetadata);
                    registerAsComponent(CONVERSATION_FILTER_CLASS, deploymentUnit);
                    webMetaData.getContextParams().add(CONVERSATION_FILTER_INITIALIZED);
                }
            }

        }
    }

    private void createDependency(DeploymentUnit deploymentUnit, WarMetaData warMetaData) {
        final ServiceName weldStartService = (deploymentUnit.getParent() != null ? deploymentUnit.getParent() : deploymentUnit).getServiceName().append(WeldStartService.SERVICE_NAME);
        warMetaData.addAdditionalDependency(weldStartService);
    }

    private void setupWeldContextIgnores(List<ParamValueMetaData> contextParams, String parameterName) {
        for (ParamValueMetaData param : contextParams) {
            if (parameterName.equals(param.getParamName())) {
                return;
            }
        }
        ParamValueMetaData parameter = new ParamValueMetaData();
        parameter.setParamName(parameterName);
        parameter.setParamValue("false");
        contextParams.add(parameter);
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}