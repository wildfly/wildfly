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

import static org.jboss.as.webservices.util.WSAttachmentKeys.JMS_ENDPOINT_METADATA_KEY;

import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.webservices.util.ASHelper;
import org.jboss.as.webservices.util.VirtualFileAdaptor;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.logging.Logger;
import org.jboss.ws.common.deployment.SOAPAddressWSDLParser;
import org.jboss.wsf.spi.deployment.UnifiedVirtualFile;
import org.jboss.wsf.spi.metadata.jms.JMSEndpointMetaData;
import org.jboss.wsf.spi.metadata.jms.JMSEndpointsMetaData;

/**
 * DUP for detecting JMS WS endpoints
 *
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
public final class WSJMSIntegrationProcessor implements DeploymentUnitProcessor {

    private static Logger LOG = Logger.getLogger(WSJMSIntegrationProcessor.class);
    private static final String WSDL_LOCATION = "wsdlLocation";
    private static final String PORT_NAME = "portName";
    private static final String SERVICE_NAME = "serviceName";
    private static final String TARGET_NAMESPACE = "targetNamespace";

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit unit = phaseContext.getDeploymentUnit();
        final List<AnnotationInstance> webServiceAnnotations = ASHelper.getAnnotations(unit, ASHelper.WEB_SERVICE_ANNOTATION);

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
        if (!map.isEmpty()) {
            final JMSEndpointsMetaData endpointsMetaData = new JMSEndpointsMetaData();
            final ResourceRoot deploymentRoot = unit.getAttachment(Attachments.DEPLOYMENT_ROOT);
            final UnifiedVirtualFile uvf = new VirtualFileAdaptor(deploymentRoot.getRoot());

            final boolean trace = LOG.isTraceEnabled();
            for (String wsdlLocation : map.keySet()) {
                if (trace) {
                    LOG.tracef("Scanning wsdlLocation: %s", wsdlLocation);
                }
                try {
                    URL url = uvf.findChild(wsdlLocation).toURL();
                    SOAPAddressWSDLParser parser = new SOAPAddressWSDLParser(url);
                    for (AnnotationInstance ai : map.get(wsdlLocation)) {
                        String port = ai.value(PORT_NAME).asString();
                        String service = ai.value(SERVICE_NAME).asString();
                        AnnotationValue targetNS = ai.value(TARGET_NAMESPACE);
                        String tns = targetNS != null ? targetNS.asString() : null;
                        QName serviceName = new QName(tns, service);
                        QName portName = new QName(tns, port);
                        if (trace) {
                            LOG.tracef("  serviceName: %s", serviceName);
                            LOG.tracef("  portName: %s", portName);
                        }
                        String soapAddress = parser.filterSoapAddress(serviceName, portName, SOAPAddressWSDLParser.SOAP_OVER_JMS_NS);
                        if (soapAddress != null) {
                            ClassInfo webServiceClassInfo = (ClassInfo) ai.target();
                            String beanClassName = webServiceClassInfo.name().toString();
                            //service name ?
                            JMSEndpointMetaData endpointMetaData = new JMSEndpointMetaData(endpointsMetaData);
                            endpointMetaData.setEndpointName(port);
                            endpointMetaData.setImplementor(beanClassName);
                            //endpointMetaData.setName(name);
                            endpointMetaData.setSoapAddress(soapAddress);
                            endpointMetaData.setWsdlLocation(wsdlLocation);
                            endpointsMetaData.addEndpointMetaData(endpointMetaData);
                        }
                    }
                } catch (Exception e) {
                    if (trace) {
                        LOG.warnf("Could not read WSDL at '%s'", wsdlLocation, e);
                    } else {
                        LOG.warnf("Could not read WSDL at '%s'", wsdlLocation);
                    }
                }
            }

            if (!endpointsMetaData.getEndpointsMetaData().isEmpty()) {
                unit.putAttachment(JMS_ENDPOINT_METADATA_KEY, endpointsMetaData);
            }
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {
        // NOOP
    }
}