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

package org.jboss.as.webservices.injection;

import static org.jboss.as.server.deployment.Attachments.ANNOTATION_INDEX;
import static org.jboss.as.webservices.util.ASHelper.isJaxwsService;
import static org.jboss.as.webservices.util.DotNames.HANDLER_CHAIN_ANNOTATION;
import static org.jboss.as.webservices.util.DotNames.WEB_SERVICE_ANNOTATION;
import static org.jboss.as.webservices.util.DotNames.WEB_SERVICE_PROVIDER_ANNOTATION;
import static org.jboss.as.webservices.util.WSAttachmentKeys.WS_ENDPOINT_HANDLERS_MAPPING_KEY;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.webservices.logging.WSLogger;
import org.jboss.as.webservices.util.ASHelper;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.vfs.VirtualFile;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerChainMetaData;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerChainsMetaData;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerChainsMetaDataParser;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerMetaData;

/**
 * Scans @HandlerChain annotations in the deployment
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
public final class WSHandlerChainAnnotationProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit unit = phaseContext.getDeploymentUnit();
        if (DeploymentTypeMarker.isType(DeploymentType.EAR, unit)) {
            return;
        }
        List<ResourceRoot> resourceRoots = ASHelper.getResourceRoots(unit);
        if (resourceRoots == null) {
            return;
        }

        final WSEndpointHandlersMapping mapping = new WSEndpointHandlersMapping();
        Index index = null;
        for (final ResourceRoot resourceRoot : resourceRoots) {
            index = resourceRoot.getAttachment(ANNOTATION_INDEX);
            if (index != null) {
                // process @HandlerChain annotations
                processHandlerChainAnnotations(resourceRoot, resourceRoots, index, mapping);
            }
        }
        if (!mapping.isEmpty()) {
            unit.putAttachment(WS_ENDPOINT_HANDLERS_MAPPING_KEY, mapping);
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {
        // noop
    }

    private static void processHandlerChainAnnotations(final ResourceRoot currentResourceRoot,
            final List<ResourceRoot> resourceRoots, final Index index, final WSEndpointHandlersMapping mapping)
            throws DeploymentUnitProcessingException {
        final List<AnnotationInstance> webServiceAnnotations = index.getAnnotations(WEB_SERVICE_ANNOTATION);
        final List<AnnotationInstance> webServiceProviderAnnotations = index.getAnnotations(WEB_SERVICE_PROVIDER_ANNOTATION);
        for (final AnnotationInstance annotationInstance : webServiceAnnotations) {
            final AnnotationTarget annotationTarget = annotationInstance.target();
            if (annotationTarget instanceof ClassInfo) {
                final ClassInfo classInfo = (ClassInfo) annotationTarget;
                if (isJaxwsEndpoint(classInfo, index)) {
                    AnnotationInstance handlerChainAnnotationInstance = getHandlerChainAnnotationInstance(classInfo);
                    //JSR-181, Section 4.6.1: "The @HandlerChain annotation MAY be present on the endpoint interface and service
                    //implementation bean. The service implementation bean’s @HandlerChain is used if @HandlerChain is present on both."
                    if (handlerChainAnnotationInstance == null) {
                        handlerChainAnnotationInstance = getEndpointInterfaceHandlerChainAnnotationInstance(classInfo, index);
                    }
                    if (handlerChainAnnotationInstance != null) {
                        final String endpointClass = classInfo.name().toString();
                        processHandlerChainAnnotation(currentResourceRoot, resourceRoots, handlerChainAnnotationInstance, endpointClass, mapping);
                    }
                }
            } else {
                // We ignore fields & methods annotated with @HandlerChain.
                // These are used always in combination with @WebServiceRef
                // which are always referencing JAXWS client proxies only.
            }
        }
        for (final AnnotationInstance annotationInstance : webServiceProviderAnnotations) {
            final AnnotationTarget annotationTarget = annotationInstance.target();
            if (annotationTarget instanceof ClassInfo) {
                final ClassInfo classInfo = (ClassInfo) annotationTarget;
                final AnnotationInstance handlerChainAnnotationInstance = getHandlerChainAnnotationInstance(classInfo);
                if (handlerChainAnnotationInstance != null && isJaxwsEndpoint(classInfo, index)) {
                    final String endpointClass = classInfo.name().toString();
                    processHandlerChainAnnotation(currentResourceRoot, resourceRoots, handlerChainAnnotationInstance, endpointClass, mapping);
                }
            } else {
                // We ignore fields & methods annotated with @HandlerChain.
                // These are used always in combination with @WebServiceRef
                // which are always referencing JAXWS client proxies only.
            }
        }
    }

    private static AnnotationInstance getHandlerChainAnnotationInstance(final ClassInfo classInfo) {
        List<AnnotationInstance> list = classInfo.annotations().get(HANDLER_CHAIN_ANNOTATION);
        return list != null && !list.isEmpty() ? list.iterator().next() : null;
    }

    private static AnnotationInstance getEndpointInterfaceHandlerChainAnnotationInstance(final ClassInfo classInfo, final Index index) {
        AnnotationValue av = classInfo.annotations().get(WEB_SERVICE_ANNOTATION).iterator().next().value("endpointInterface");
        if (av != null) {
            String intf = av.asString();
            if (intf != null && !intf.isEmpty()) {
                ClassInfo intfClassInfo = index.getClassByName(DotName.createSimple(intf));
                if (intfClassInfo != null && ASHelper.isJaxwsEndpointInterface(intfClassInfo)) {
                    return getHandlerChainAnnotationInstance(intfClassInfo);
                }
            }
        }
        return null;
    }

    private static void processHandlerChainAnnotation(final ResourceRoot currentResourceRoot,
            final List<ResourceRoot> resourceRoots, final AnnotationInstance handlerChainAnnotation,
            final String endpointClass, final WSEndpointHandlersMapping mapping) throws DeploymentUnitProcessingException {
        final String handlerChainConfigFile = handlerChainAnnotation.value("file").asString();
        InputStream is = null;
        try {
            is = getInputStream(currentResourceRoot, resourceRoots, handlerChainConfigFile, endpointClass);
            final Set<String> endpointHandlers = getHandlers(is);
            if (endpointHandlers.size() > 0) {
                mapping.registerEndpointHandlers(endpointClass, endpointHandlers);
            } else {
                WSLogger.ROOT_LOGGER.invalidHandlerChainFile(handlerChainConfigFile);
            }
        } catch (final IOException e) {
            throw new DeploymentUnitProcessingException(e);
        } finally {
            if (is != null) {
                try { is.close(); } catch (final IOException ignore) {}
            }
        }
    }

    private static InputStream getInputStream(final ResourceRoot currentResourceRoot, final List<ResourceRoot> resourceRoots, final String handlerChainConfigFile, final String annotatedClassName) throws IOException {
        if (handlerChainConfigFile.startsWith("file://") || handlerChainConfigFile.startsWith("http://")) {
            return new URL(handlerChainConfigFile).openStream();
        } else {
            URI classURI = null;
            try {
                classURI = new URI(annotatedClassName.replace('.', '/'));
            } catch (final URISyntaxException ignore) {}
            final String handlerChainConfigFileResourcePath = classURI.resolve(handlerChainConfigFile).toString();
            VirtualFile config = currentResourceRoot.getRoot().getChild(handlerChainConfigFileResourcePath);
            if (config.exists() && config.isFile()) {
                return config.openStream();
            } else {
                for (ResourceRoot rr : resourceRoots) {
                    config = rr.getRoot().getChild(handlerChainConfigFileResourcePath);
                    if (config.exists() && config.isFile()) {
                        return config.openStream();
                    }
                }
            }

            throw WSLogger.ROOT_LOGGER.missingHandlerChainConfigFile(handlerChainConfigFileResourcePath, currentResourceRoot);
        }
    }

    private static Set<String> getHandlers(final InputStream is) throws IOException {
        final Set<String> retVal = new HashSet<String>();

        final UnifiedHandlerChainsMetaData handlerChainsUMDM = UnifiedHandlerChainsMetaDataParser.parse(is);
        if (handlerChainsUMDM != null) {
            for (final UnifiedHandlerChainMetaData handlerChainUMDM : handlerChainsUMDM.getHandlerChains()) {
                for (final UnifiedHandlerMetaData handlerUMDM : handlerChainUMDM.getHandlers()) {
                    retVal.add(handlerUMDM.getHandlerClass());
                }
            }
        }
        return retVal;
    }

    private static boolean isJaxwsEndpoint(final ClassInfo clazz, final Index index) {
        // assert JAXWS endpoint class flags
        final short flags = clazz.flags();
        if (Modifier.isInterface(flags)) return false;
        if (Modifier.isAbstract(flags)) return false;
        if (!Modifier.isPublic(flags)) return false;
        if (isJaxwsService(clazz, index)) return false;
        if (Modifier.isFinal(flags)) return false;
        final boolean isWebService = clazz.annotations().containsKey(WEB_SERVICE_ANNOTATION);
        final boolean isWebServiceProvider = clazz.annotations().containsKey(WEB_SERVICE_PROVIDER_ANNOTATION);
        return isWebService || isWebServiceProvider;
    }
}