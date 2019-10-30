/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.webservices.deployers;

import static org.jboss.as.server.deployment.Attachments.DEPLOYMENT_ROOT;
import static org.jboss.as.server.deployment.Attachments.RESOURCE_ROOTS;
import static org.jboss.as.webservices.util.ASHelper.getAnnotations;
import static org.jboss.as.webservices.util.DotNames.WEB_SERVICE_ANNOTATION;
import static org.jboss.as.webservices.util.WSAttachmentKeys.JMS_ENDPOINT_METADATA_KEY;

import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.webservices.logging.WSLogger;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.vfs.VirtualFile;
import org.jboss.ws.common.deployment.SOAPAddressWSDLParser;
import org.jboss.wsf.spi.metadata.jms.JMSEndpointMetaData;
import org.jboss.wsf.spi.metadata.jms.JMSEndpointsMetaData;

/**
 * DUP for detecting JMS WS endpoints
 *
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
public final class WSIntegrationProcessorJAXWS_JMS implements DeploymentUnitProcessor {

    private static final String WSDL_LOCATION = "wsdlLocation";
    private static final String PORT_NAME = "portName";
    private static final String SERVICE_NAME = "serviceName";
    private static final String TARGET_NAMESPACE = "targetNamespace";

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit unit = phaseContext.getDeploymentUnit();
        if (DeploymentTypeMarker.isType(DeploymentType.EAR, unit)) {
            return;
        }
        final List<AnnotationInstance> webServiceAnnotations = getAnnotations(unit, WEB_SERVICE_ANNOTATION);
        // TODO: how about @WebServiceProvider JMS based endpoints?

        //group @WebService annotations in the deployment by wsdl contract location
        Map<String, List<AnnotationInstance>> map = new HashMap<String, List<AnnotationInstance>>();
        for (AnnotationInstance webServiceAnnotation : webServiceAnnotations) {
            final AnnotationValue wsdlLocation = webServiceAnnotation.value(WSDL_LOCATION);
            final AnnotationValue port = webServiceAnnotation.value(PORT_NAME);
            final AnnotationValue service = webServiceAnnotation.value(SERVICE_NAME);
            //support for contract-first development only: pick-up @WebService annotations referencing a provided wsdl contract only
            if (wsdlLocation != null && port != null && service != null) {
                String key = wsdlLocation.asString();
                List<AnnotationInstance> annotations = map.get(key);
                if (annotations == null) {
                    annotations = new LinkedList<AnnotationInstance>();
                    map.put(key, annotations);
                }
                annotations.add(webServiceAnnotation);
            }
        }

        //extract SOAP-over-JMS 1.0 bindings
        List<JMSEndpointMetaData> list = new LinkedList<JMSEndpointMetaData>();
        if (!map.isEmpty()) {

            for (String wsdlLocation : map.keySet()) {
                try {
                    final ResourceRoot resourceRoot = getWsdlResourceRoot(unit, wsdlLocation);
                    if (resourceRoot == null) continue;
                    final VirtualFile wsdlLocationFile = resourceRoot.getRoot().getChild(wsdlLocation);
                    final URL url = wsdlLocationFile.toURL();
                    SOAPAddressWSDLParser parser = new SOAPAddressWSDLParser(url);
                    for (AnnotationInstance ai : map.get(wsdlLocation)) {
                        String port = ai.value(PORT_NAME).asString();
                        String service = ai.value(SERVICE_NAME).asString();
                        AnnotationValue targetNS = ai.value(TARGET_NAMESPACE);
                        String tns = targetNS != null ? targetNS.asString() : null;
                        QName serviceName = new QName(tns, service);
                        QName portName = new QName(tns, port);
                        String soapAddress = parser.filterSoapAddress(serviceName, portName, SOAPAddressWSDLParser.SOAP_OVER_JMS_NS);
                        if (soapAddress != null) {
                            ClassInfo webServiceClassInfo = (ClassInfo) ai.target();
                            String beanClassName = webServiceClassInfo.name().toString();
                            //service name ?
                            list.add(new JMSEndpointMetaData(beanClassName, port, beanClassName, wsdlLocation, soapAddress));
                        }
                    }
                } catch (Exception ignore) {
                    WSLogger.ROOT_LOGGER.cannotReadWsdl(wsdlLocation);
                }
            }

        }
        unit.putAttachment(JMS_ENDPOINT_METADATA_KEY, new JMSEndpointsMetaData(list));
    }

    @Override
    public void undeploy(final DeploymentUnit context) {
        // NOOP
    }

    private static ResourceRoot getWsdlResourceRoot(final DeploymentUnit unit, final String wsdlPath) {
        final AttachmentList<ResourceRoot> resourceRoots = new AttachmentList<ResourceRoot>(ResourceRoot.class);
        final ResourceRoot root = unit.getAttachment(DEPLOYMENT_ROOT);
        resourceRoots.add(root);
        final AttachmentList<ResourceRoot> otherResourceRoots = unit.getAttachment(RESOURCE_ROOTS);
        if (otherResourceRoots != null) {
            resourceRoots.addAll(otherResourceRoots);
        }
        for (final ResourceRoot resourceRoot : resourceRoots) {
            VirtualFile file = resourceRoot.getRoot().getChild(wsdlPath);
            if (file.exists()) return resourceRoot;
        }
        return null;
    }

}