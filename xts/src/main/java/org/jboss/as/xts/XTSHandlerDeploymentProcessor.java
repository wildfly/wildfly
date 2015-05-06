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

import com.arjuna.mw.wst11.service.JaxWSHeaderContextProcessor;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.xts.jandex.CompensatableAnnotation;
import org.jboss.as.xts.jandex.EndpointMetaData;
import org.jboss.as.xts.jandex.OldCompensatableAnnotation;
import org.jboss.as.xts.jandex.TransactionalAnnotation;
import org.jboss.as.webservices.injection.WSEndpointHandlersMapping;
import org.jboss.as.webservices.util.ASHelper;
import org.jboss.as.webservices.util.WSAttachmentKeys;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jbossts.txbridge.inbound.OptionalJaxWSTxInboundBridgeHandler;
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

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 * @author <a href="mailto:paul.robinson@redhat.com">Paul Robinson</a>
 */
public class XTSHandlerDeploymentProcessor implements DeploymentUnitProcessor {

    private static final String TX_BRIDGE_HANDLER = OptionalJaxWSTxInboundBridgeHandler.class.getName();

    private static final String TX_CONTEXT_HANDLER = JaxWSHeaderContextProcessor.class.getName();

    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit unit = phaseContext.getDeploymentUnit();
        final List<WebserviceDescriptionMetaData> webserviceDescriptions = new ArrayList<WebserviceDescriptionMetaData>();
        boolean modifiedWSMeta = false;

        for (String endpoint : getDeploymentClasses(unit)) {
            try {
                final EndpointMetaData endpointMetaData = EndpointMetaData.build(unit, endpoint);

                if (endpointMetaData.isXTSEnabled()) {
                    XTSDeploymentMarker.mark(unit);
                    final boolean result = updateXTSEndpoint(endpoint, endpointMetaData, webserviceDescriptions, unit);
                    modifiedWSMeta = modifiedWSMeta || result;
                }
            } catch (XTSException e) {
                throw new DeploymentUnitProcessingException("Error processing endpoint '" + endpoint + "'", e);
            }
        }

        if (modifiedWSMeta) {
            unit.putAttachment(WSAttachmentKeys.WEBSERVICES_METADATA_KEY, new WebservicesMetaData(null, webserviceDescriptions));
        }
    }

    private boolean updateXTSEndpoint(final String endpoint, final EndpointMetaData endpointMetaData,
            final List<WebserviceDescriptionMetaData> webserviceDescriptions, final DeploymentUnit unit) {

        if (endpointMetaData.isWebservice()) {
            final List<String> handlers = new ArrayList<String>();

            if (endpointMetaData.isBridgeEnabled()) {
                handlers.add(TX_BRIDGE_HANDLER);
            }
            handlers.add(TX_CONTEXT_HANDLER);

            if (!isAnyOfHandlersRegistered(unit, endpoint, handlers)) {
                final UnifiedHandlerChainsMetaData unifiedHandlerChainsMetaData = buildHandlerChains(handlers);
                final QName portQname = endpointMetaData.getWebServiceAnnotation().buildPortQName();
                webserviceDescriptions.add(new WebserviceDescriptionMetaData(null, null, null,
                        buildPortComponent(endpointMetaData.isEJB(), endpoint, portQname, unifiedHandlerChainsMetaData)));
                registerHandlersWithAS(unit, endpoint, handlers);

                return true;
            }
        }

        return false;
    }

    private PortComponentMetaData buildPortComponent(boolean isEJB, String endpointClass, QName portQname, UnifiedHandlerChainsMetaData unifiedHandlerChainsMetaData) {
        return new PortComponentMetaData(null, portQname, endpointClass, isEJB ? getClassName(endpointClass) : null, isEJB ? null : endpointClass, null, null, null, null, unifiedHandlerChainsMetaData);
    }

    private UnifiedHandlerChainsMetaData buildHandlerChains(List<String> handlerClasses) {

        List<UnifiedHandlerMetaData> handlers = new ArrayList<UnifiedHandlerMetaData>();
        for (String handlerClass : handlerClasses) {
            handlers.add(new UnifiedHandlerMetaData(handlerClass, null, null, null, null, null));
        }
        return new UnifiedHandlerChainsMetaData(new UnifiedHandlerChainMetaData(null, null, null, handlers, false, null));
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

        for (final String annotation : CompensatableAnnotation.COMPENSATABLE_ANNOTATIONS) {
            addEndpointsToList(endpoints, ASHelper.getAnnotations(unit, DotName.createSimple(annotation)));
        }

        for (final String annotation : TransactionalAnnotation.TRANSACTIONAL_ANNOTATIONS) {
            addEndpointsToList(endpoints, ASHelper.getAnnotations(unit, DotName.createSimple(annotation)));
        }

        for (final String annotation : OldCompensatableAnnotation.COMPENSATABLE_ANNOTATIONS) {
            addEndpointsToList(endpoints, ASHelper.getAnnotations(unit, DotName.createSimple(annotation)));
        }

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

    private boolean isAnyOfHandlersRegistered(final DeploymentUnit unit, final String endpointClass,
            final List<String> handlers) {

        final WSEndpointHandlersMapping mapping = unit.getAttachment(WSAttachmentKeys.WS_ENDPOINT_HANDLERS_MAPPING_KEY);

        if (mapping == null) {
            return false;
        }

        final Set<String> existingHandlers = mapping.getHandlers(endpointClass);

        if (existingHandlers == null) {
            return false;
        }

        for (final String handler : handlers) {
            if (existingHandlers.contains(handler)) {
                return true;
            }
        }

        return false;
    }

    public void undeploy(final DeploymentUnit unit) {

        // does nothing
    }

}
