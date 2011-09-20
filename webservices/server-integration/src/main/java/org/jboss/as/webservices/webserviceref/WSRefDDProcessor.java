/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.webservices.webserviceref;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.DeploymentDescriptorEnvironment;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.deployers.AbstractDeploymentDescriptorBindingsProcessor;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.as.webservices.util.VirtualFileAdaptor;
import org.jboss.metadata.javaee.jboss.JBossPortComponentRef;
import org.jboss.metadata.javaee.jboss.JBossServiceReferenceMetaData;
import org.jboss.metadata.javaee.jboss.StubPropertyMetaData;
import org.jboss.metadata.javaee.spec.Addressing;
import org.jboss.metadata.javaee.spec.PortComponentRef;
import org.jboss.metadata.javaee.spec.ServiceReferenceMetaData;
import org.jboss.metadata.javaee.spec.ServiceReferencesMetaData;
import org.jboss.wsf.spi.deployment.UnifiedVirtualFile;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedPortComponentRefMetaData;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedServiceRefMetaData;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedStubPropertyMetaData;
import org.jboss.wsf.spi.serviceref.ServiceRefHandler;

/**
 * TODO: javadoc
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class WSRefDDProcessor extends AbstractDeploymentDescriptorBindingsProcessor {

    @Override
    protected List<BindingConfiguration> processDescriptorEntries(final DeploymentUnit unit,
            final DeploymentDescriptorEnvironment environment, final EEModuleDescription moduleDescription,
            final ComponentDescription componentDescription, final ClassLoader classLoader,
            final DeploymentReflectionIndex deploymentReflectionIndex, final EEApplicationClasses applicationClasses)
            throws DeploymentUnitProcessingException {
        final ServiceReferencesMetaData serviceRefsMD = environment.getEnvironment().getServiceReferences();
        if (serviceRefsMD == null) {
            return Collections.<BindingConfiguration> emptyList();
        }

        final List<BindingConfiguration> bindingDescriptions = new LinkedList<BindingConfiguration>();
        for (final ServiceReferenceMetaData serviceRefMD : serviceRefsMD) {
            final UnifiedServiceRefMetaData serviceRefUMDM = toUMDM(serviceRefMD, unit, classLoader);
            final WSRefValueSource valueSource = new WSRefValueSource(serviceRefUMDM);
            final BindingConfiguration bindingConfiguration = new BindingConfiguration(serviceRefMD.getName(), valueSource);
            bindingDescriptions.add(bindingConfiguration);
        }
        return bindingDescriptions;
    }

    private static UnifiedServiceRefMetaData toUMDM(final ServiceReferenceMetaData serviceRefMD,
            final DeploymentUnit unit, final ClassLoader classLoader) {
        final UnifiedServiceRefMetaData serviceRefUMDM = new UnifiedServiceRefMetaData(getUnifiedVirtualFile(unit));
        serviceRefUMDM.setServiceRefName(serviceRefMD.getName());
        serviceRefUMDM.setServiceQName(serviceRefMD.getServiceQname());
        serviceRefUMDM.setServiceInterface(serviceRefMD.getServiceInterface());
        serviceRefUMDM.setServiceRefType(serviceRefMD.getServiceRefType()); // TODO: correct ?
        serviceRefUMDM.setWsdlFile(serviceRefMD.getWsdlFile());
        serviceRefUMDM.setMappingFile(serviceRefMD.getJaxrpcMappingFile());
        // propagate port compoments

        final Collection<? extends PortComponentRef> portComponentsMD = serviceRefMD.getPortComponentRef();
        if (portComponentsMD != null) {
            for (final PortComponentRef portComponentMD : portComponentsMD) {
                final UnifiedPortComponentRefMetaData portComponentUMDM = getUnifiedPortComponentRefMetaData(serviceRefUMDM,
                        portComponentMD);
                if (portComponentUMDM.getServiceEndpointInterface() != null || portComponentUMDM.getPortQName() != null) {
                    serviceRefUMDM.addPortComponentRef(portComponentUMDM);
                } else {
                    // log.warn(BundleUtils.getMessage(bundle, "IGNORING_PORT_REF", portComponentUMDM));
                }
            }
        }
        // propagate jboss specific MD
        if (serviceRefMD instanceof JBossServiceReferenceMetaData) {
           processUnifiedJBossServiceRefMetaData(serviceRefUMDM, serviceRefMD);
        }

        /*
        final Class<?> typeClass;
        try {
            typeClass = classLoader.loadClass(type);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not load class " + type);
        }

        serviceRefUMDM.setServiceRefType(type);
        if (!isEmpty(value)) {
            serviceRefUMDM.setServiceInterface(value);
        } else if (Service.class.isAssignableFrom(typeClass)) {
            serviceRefUMDM.setServiceInterface(type);
        } else {
            serviceRefUMDM.setServiceInterface(Service.class.getName());
        }*/

        final boolean isJAXRPC = serviceRefUMDM.getMappingFile() != null // TODO: is mappingFile check required?
        || "javax.xml.rpc.Service".equals(serviceRefUMDM.getServiceInterface());
        serviceRefUMDM.setType(isJAXRPC ? ServiceRefHandler.Type.JAXRPC : ServiceRefHandler.Type.JAXWS); // TODO: service ref type - later in the DA chain

        System.out.println("---------------------------");
        System.out.println("---------------------------");
        System.out.println(serviceRefUMDM);
        System.out.println("---------------------------");
        System.out.println("---------------------------");

        return serviceRefUMDM;
    }

    private static void processUnifiedJBossServiceRefMetaData(final UnifiedServiceRefMetaData serviceRefUMDM, final ServiceReferenceMetaData serviceRefMD) { final JBossServiceReferenceMetaData jbossServiceRefMD = (JBossServiceReferenceMetaData) serviceRefMD;
        serviceRefUMDM.setServiceImplClass(jbossServiceRefMD.getServiceClass());
        serviceRefUMDM.setConfigName(jbossServiceRefMD.getConfigName());
        serviceRefUMDM.setConfigFile(jbossServiceRefMD.getConfigFile());
        serviceRefUMDM.setWsdlOverride(jbossServiceRefMD.getWsdlOverride());
        serviceRefUMDM.setHandlerChain(jbossServiceRefMD.getHandlerChain());
    }

    private static UnifiedPortComponentRefMetaData getUnifiedPortComponentRefMetaData(final UnifiedServiceRefMetaData serviceRefUMDM, final PortComponentRef portComponentMD) {
        final UnifiedPortComponentRefMetaData portComponentUMDM = new UnifiedPortComponentRefMetaData(serviceRefUMDM);

        // propagate service endpoint interface
        portComponentUMDM.setServiceEndpointInterface(portComponentMD.getServiceEndpointInterface());

        // propagate MTOM properties
        portComponentUMDM.setMtomEnabled(portComponentMD.isEnableMtom()); // TODO: refactor method name
        portComponentUMDM.setMtomThreshold(portComponentMD.getMtomThreshold());

        // propagate addressing properties
        final Addressing addressingSBMD = portComponentMD.getAddressing();
        if (addressingSBMD != null) {
            portComponentUMDM.setAddressingAnnotationSpecified(true);
            portComponentUMDM.setAddressingEnabled(addressingSBMD.isEnabled());
            portComponentUMDM.setAddressingRequired(addressingSBMD.isRequired());
            portComponentUMDM.setAddressingResponses(addressingSBMD.getResponses());
        }

        // propagate respect binding properties
        if (portComponentMD.getRespectBinding() != null) {
            portComponentUMDM.setRespectBindingAnnotationSpecified(true);
            portComponentUMDM.setRespectBindingEnabled(true);
        }

        // propagate link
        portComponentUMDM.setPortComponentLink(portComponentMD.getPortComponentLink());

        // propagate jboss specific MD
        if (portComponentMD instanceof JBossPortComponentRef) {
            processUnifiedJBossPortComponentRefMetaData(portComponentUMDM, portComponentMD);
        }

        return portComponentUMDM;
    }

    private static void processUnifiedJBossPortComponentRefMetaData(final UnifiedPortComponentRefMetaData portComponentUMDM, final PortComponentRef portComponentMD) {
        final JBossPortComponentRef jbossPortComponentMD = (JBossPortComponentRef) portComponentMD;

        // propagate port QName
        portComponentUMDM.setPortQName(jbossPortComponentMD.getPortQname()); // TODO: fix method name

        // propagate configuration properties
        portComponentUMDM.setConfigName(jbossPortComponentMD.getConfigName());
        portComponentUMDM.setConfigFile(jbossPortComponentMD.getConfigFile());

        // propagate stub properties
        final List<StubPropertyMetaData> stubPropertiesMD = jbossPortComponentMD.getStubProperties();
        if (stubPropertiesMD != null) {
            for (final StubPropertyMetaData stubPropertyMD : stubPropertiesMD) {
                final UnifiedStubPropertyMetaData stubPropertyUMDM = new UnifiedStubPropertyMetaData();
                stubPropertyUMDM.setPropName(stubPropertyMD.getPropName());
                stubPropertyUMDM.setPropValue(stubPropertyMD.getPropValue());
                portComponentUMDM.addStubProperty(stubPropertyUMDM);
            }
        }
    }

    private static UnifiedVirtualFile getUnifiedVirtualFile(final DeploymentUnit deploymentUnit) { // TODO: refactor to common code
        ResourceRoot resourceRoot = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.DEPLOYMENT_ROOT);
        if (resourceRoot == null) {
            throw new IllegalStateException("Resource root not found for deployment " + deploymentUnit);
        }
        return new VirtualFileAdaptor(resourceRoot.getRoot());
    }

}
