/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.xts;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.xts.jandex.BridgeType;
import org.jboss.as.xts.jandex.CompensatableAnnotation;
import org.jboss.as.xts.jandex.OldCompensatableAnnotation;
import org.jboss.as.xts.jandex.EndpointMetaData;
import org.jboss.as.xts.jandex.TransactionalAnnotation;
import org.jboss.as.webservices.injection.WSEndpointHandlersMapping;
import org.jboss.as.webservices.util.ASHelper;
import org.jboss.as.webservices.util.WSAttachmentKeys;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerChainMetaData;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerChainsMetaData;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerMetaData;
import org.jboss.wsf.spi.metadata.webservices.PortComponentMetaData;
import org.jboss.wsf.spi.metadata.webservices.WebserviceDescriptionMetaData;
import org.jboss.wsf.spi.metadata.webservices.WebservicesMetaData;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class XTSHandlerDeploymentProcessor implements DeploymentUnitProcessor {

    private static final String TX_BRIDGE_HANDLER = "org.jboss.jbossts.txbridge.inbound.JaxWSTxInboundBridgeHandler";
    private static final String TX_CONTEXT_HANDLER = "com.arjuna.mw.wst11.service.JaxWSHeaderContextProcessor";

    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit unit = phaseContext.getDeploymentUnit();

        WebservicesMetaData webservicesMetaData = new WebservicesMetaData();

        boolean modifiedWSMeta = false;

        for (String endpoint : getDeploymentClasses(unit)) {
            try {

                EndpointMetaData endpointMetaData = EndpointMetaData.build(unit, endpoint);

                if (endpointMetaData.isTXFrameworkEnabled()) {

                    XTSDeploymentMarker.mark(unit);

                    if (endpointMetaData.isWebservice()) {

                        List<String> handlers = new ArrayList<String>();
                        TransactionalAnnotation TransactionalAnnotation = endpointMetaData.getTransactionalAnnotation();
                        if (shouldBridge(TransactionalAnnotation)) {
                            handlers.add(TX_BRIDGE_HANDLER);
                        }
                        handlers.add(TX_CONTEXT_HANDLER);

                        addHandlerToEndpoint(webservicesMetaData, endpointMetaData, endpoint, handlers);
                        registerHandlersWithAS(unit, endpoint, handlers);
                        modifiedWSMeta = true;
                    }

                }

            } catch (XTSException e) {
                throw new DeploymentUnitProcessingException("Error processing endpoint '" + endpoint + "'", e);
            }
        }

        if (modifiedWSMeta) {
            unit.putAttachment(WSAttachmentKeys.WEBSERVICES_METADATA_KEY, webservicesMetaData);
        }
    }

    private boolean shouldBridge(TransactionalAnnotation TransactionalAnnotation) {
        if (TransactionalAnnotation == null) {
            return false;
        }
        if (TransactionalAnnotation.getBridgeType() == null) {
            return false;
        }
        BridgeType bridgeType = TransactionalAnnotation.getBridgeType();
        return (bridgeType.equals(BridgeType.JTA) || bridgeType.equals(BridgeType.DEFAULT));
    }

    private void addHandlerToEndpoint(WebservicesMetaData wsWebservicesMetaData, EndpointMetaData endpointMetaData, String endpointClass, List<String> handlers) {

        WebserviceDescriptionMetaData descriptionMetaData = new WebserviceDescriptionMetaData(wsWebservicesMetaData);

        final UnifiedHandlerChainsMetaData unifiedHandlerChainsMetaData = buildHandlerChains(handlers);
        final QName portQname = endpointMetaData.getWebServiceAnnotation().buildPortQName();
        final PortComponentMetaData portComponent = buildPortComponent(endpointMetaData.isEJB(), endpointClass, portQname, unifiedHandlerChainsMetaData, descriptionMetaData);
        descriptionMetaData.addPortComponent(portComponent);
        wsWebservicesMetaData.addWebserviceDescription(descriptionMetaData);
    }

    private PortComponentMetaData buildPortComponent(boolean isEJB, String endpointClass, QName portQname, UnifiedHandlerChainsMetaData unifiedHandlerChainsMetaData, WebserviceDescriptionMetaData descriptionMetaData) {

        PortComponentMetaData portComponent = new PortComponentMetaData(descriptionMetaData);
        portComponent.setHandlerChains(unifiedHandlerChainsMetaData);
        portComponent.setServiceEndpointInterface(endpointClass);
        portComponent.setWsdlPort(portQname);

        if (isEJB) {
            portComponent.setEjbLink(getClassName(endpointClass));
        } else {
            portComponent.setServletLink(endpointClass);
        }

        return portComponent;
    }

    private UnifiedHandlerChainsMetaData buildHandlerChains(List<String> handlerClasses) {

        UnifiedHandlerChainMetaData unifiedHandlerChainMetaData = new UnifiedHandlerChainMetaData();

        for (String handlerClass : handlerClasses) {
            UnifiedHandlerMetaData handlerMetaData = new UnifiedHandlerMetaData();
            handlerMetaData.setHandlerClass(handlerClass);
            unifiedHandlerChainMetaData.addHandler(handlerMetaData);
        }

        UnifiedHandlerChainsMetaData unifiedHandlerChainsMetaData = new UnifiedHandlerChainsMetaData();
        unifiedHandlerChainsMetaData.addHandlerChain(unifiedHandlerChainMetaData);

        return unifiedHandlerChainsMetaData;
    }

    private void registerHandlersWithAS(DeploymentUnit unit, String endpointClass, List<String> handlersToAdd) {

        WSEndpointHandlersMapping mapping = unit.getAttachment(WSAttachmentKeys.WS_ENDPOINT_HANDLERS_MAPPING_KEY);
        if (mapping == null) {
            mapping = new WSEndpointHandlersMapping();
            unit.putAttachment(WSAttachmentKeys.WS_ENDPOINT_HANDLERS_MAPPING_KEY, mapping);
        }

        Set<String> existingHandlers = mapping.getHandlers(endpointClass);
        if (existingHandlers == null) {
            existingHandlers = new HashSet<String>();
        } else {
            //Existing collection is an unmodifiableSet
            existingHandlers = new HashSet<String>(existingHandlers);
        }

        for (String handler : handlersToAdd) {
            if (existingHandlers.contains(handler)) {
                return;
            }
            existingHandlers.add(handler);
        }
        mapping.registerEndpointHandlers(endpointClass, existingHandlers);
    }

    private String getClassName(String fQClass) {
        String[] split = fQClass.split("\\.");
        return split[split.length - 1];
    }

    private Set<String> getDeploymentClasses(DeploymentUnit unit) {

        final Set<String> endpoints = new HashSet<String>();
        addEndpointsToList(endpoints, ASHelper.getAnnotations(unit, DotName.createSimple(OldCompensatableAnnotation.COMPENSATABLE_ANNOTATION)));
        addEndpointsToList(endpoints, ASHelper.getAnnotations(unit, DotName.createSimple(CompensatableAnnotation.COMPENSATABLE_ANNOTATION)));
        addEndpointsToList(endpoints, ASHelper.getAnnotations(unit, DotName.createSimple(TransactionalAnnotation.TRANSACTIONAL_ANNOTATION)));
        return endpoints;
    }

    private void addEndpointsToList(Set<String> endpoints, List<AnnotationInstance> annotations) {
        for (AnnotationInstance annotationInstance : annotations) {

            Object target = annotationInstance.target();
            if (target instanceof ClassInfo) {

                final ClassInfo classInfo = (ClassInfo) annotationInstance.target();
                final String endpointClass = classInfo.name().toString();
                endpoints.add(endpointClass);

            } else if (target instanceof MethodInfo) {

                final MethodInfo methodInfo = (MethodInfo) target;
                final String endpointClass = methodInfo.declaringClass().name().toString();
                endpoints.add(endpointClass);
            }
        }
    }

    public void undeploy(final DeploymentUnit unit) {

        // does nothing
    }

}
