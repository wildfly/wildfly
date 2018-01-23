/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.deployment.processors;


import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.metadata.MetadataCompleteMarker;
import org.jboss.as.ee.structure.JBossDescriptorPropertyReplacement;
import org.jboss.as.ee.structure.SpecDescriptorPropertyReplacement;
import org.jboss.as.ejb3.cache.EJBBoundCacheParser;
import org.jboss.as.ejb3.clustering.ClusteringSchema;
import org.jboss.as.ejb3.clustering.EJBBoundClusteringMetaDataParser;
import org.jboss.as.ejb3.deliveryactive.parser.EJBBoundMdbDeliveryMetaDataParser;
import org.jboss.as.ejb3.deliveryactive.parser.EJBBoundMdbDeliveryMetaDataParser11;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.ejb3.deployment.EjbJarDescription;
import org.jboss.as.ejb3.interceptor.ContainerInterceptorsParser;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.pool.EJBBoundPoolParser;
import org.jboss.as.ejb3.resourceadapterbinding.parser.EJBBoundResourceAdapterBindingMetaDataParser;
import org.jboss.as.ejb3.security.parser.EJBBoundSecurityMetaDataParser;
import org.jboss.as.ejb3.security.parser.EJBBoundSecurityMetaDataParser11;
import org.jboss.as.ejb3.security.parser.SecurityRoleMetaDataParser;
import org.jboss.as.ejb3.timerservice.TimerServiceMetaDataParser;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.EjbDeploymentMarker;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.metadata.ejb.parser.jboss.ejb3.IIOPMetaDataParser;
import org.jboss.metadata.ejb.parser.jboss.ejb3.JBossEjb3MetaDataParser;
import org.jboss.metadata.ejb.parser.jboss.ejb3.TransactionTimeoutMetaDataParser;
import org.jboss.metadata.ejb.parser.spec.AbstractMetaDataParser;
import org.jboss.metadata.ejb.parser.spec.EjbJarMetaDataParser;
import org.jboss.metadata.ejb.spec.AbstractEnterpriseBeanMetaData;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;
import org.jboss.metadata.ejb.spec.EjbType;
import org.jboss.metadata.parser.util.MetaDataElementParser;
import org.jboss.vfs.VirtualFile;

/**
 * Processes a {@link DeploymentUnit} containing an ejb-jar.xml and creates {@link EjbJarMetaData}
 * for that unit.
 * <p/>
 * This {@link DeploymentUnitProcessor deployment unit processor} looks for ejb-jar.xml in META-INF of a .jar
 * and WEB-INF of a .war file. If it finds the ejb-jar.xml in these locations, it parses that file and creates
 * {@link EjbJarMetaData} out of it. The {@link EjbJarMetaData} is then attached to the {@link DeploymentUnit}
 * with {@link org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys#EJB_JAR_METADATA} as the key.
 * <p/>
 * <p/>
 * Author: Jaikiran Pai
 */
public class EjbJarParsingDeploymentUnitProcessor implements DeploymentUnitProcessor {

    /**
     * .war file extension
     */
    private static final String WAR_FILE_EXTENSION = ".war";

    /**
     * .jar file extension
     */
    private static final String JAR_FILE_EXTENSION = ".jar";

    private static final String EJB_JAR_XML = "ejb-jar.xml";
    private static final String JBOSS_EJB3_XML = "jboss-ejb3.xml";
    private static final String META_INF = "META-INF";
    private static final String WEB_INF = "WEB-INF";

    /**
     * Finds an ejb-jar.xml (at WEB-INF of a .war or META-INF of a .jar) parses the file and creates
     * metadata out of it. The metadata is then attached to the deployment unit.
     *
     * @param deploymentPhase
     * @throws DeploymentUnitProcessingException
     *
     */
    @Override
    public void deploy(DeploymentPhaseContext deploymentPhase) throws DeploymentUnitProcessingException {

        // get hold of the deployment unit.
        final DeploymentUnit deploymentUnit = deploymentPhase.getDeploymentUnit();

        // get the root of the deployment unit

        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
        final EEApplicationClasses applicationClassesDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_APPLICATION_CLASSES_DESCRIPTION);

        final EjbJarMetaData ejbJarMetaData;
        final EjbJarMetaData specMetaData = parseEjbJarXml(deploymentUnit);
        final EjbJarMetaData jbossMetaData = parseJBossEjb3Xml(deploymentUnit);
        if (specMetaData == null) {
            if (jbossMetaData == null)
                return;
            ejbJarMetaData = jbossMetaData;
        } else if (jbossMetaData == null) {
            ejbJarMetaData = specMetaData;
        } else {
            ejbJarMetaData = jbossMetaData.createMerged(specMetaData);
        }

        // Mark it as an EJB deployment
        EjbDeploymentMarker.mark(deploymentUnit);
        if (!deploymentUnit.hasAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_DESCRIPTION)) {
            final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
            final EjbJarDescription ejbModuleDescription = new EjbJarDescription(moduleDescription, deploymentUnit.getName().endsWith(".war"));
            deploymentUnit.putAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_DESCRIPTION, ejbModuleDescription);
        }

        // attach the EjbJarMetaData to the deployment unit
        deploymentUnit.putAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_METADATA, ejbJarMetaData);

        // if the jboss-ejb3.xml has a distinct-name configured then attach it to the deployment unit
        if (jbossMetaData != null && jbossMetaData.getDistinctName() != null) {
            deploymentUnit.putAttachment(org.jboss.as.ee.structure.Attachments.DISTINCT_NAME, jbossMetaData.getDistinctName());
        }

        if (ejbJarMetaData.getModuleName() != null) {
            eeModuleDescription.setModuleName(ejbJarMetaData.getModuleName());
        }
        if (ejbJarMetaData.isMetadataComplete()) {
            MetadataCompleteMarker.setMetadataComplete(deploymentUnit, true);
        }
        if (!ejbJarMetaData.isEJB3x()) {
            //EJB spec 20.5.1, we do not process annotations for older deployments
            MetadataCompleteMarker.setMetadataComplete(deploymentUnit, true);
        }

        if(ejbJarMetaData.getEnterpriseBeans() != null) {
            //check for entity beans
            StringBuilder beans = new StringBuilder();
            boolean error = false;
            for (AbstractEnterpriseBeanMetaData bean : ejbJarMetaData.getEnterpriseBeans()) {
                if (bean.getEjbType() == EjbType.ENTITY) {
                    if (!error) {
                        error = true;
                    } else {
                        beans.append(", ");
                    }
                    beans.append(bean.getEjbName());
                }
            }
            if (error) {
                throw EjbLogger.ROOT_LOGGER.entityBeansAreNotSupported(beans.toString());
            }
        }

    }

    /**
     * @param unit
     */
    @Override
    public void undeploy(DeploymentUnit unit) {

    }

    private static VirtualFile getDescriptor(final VirtualFile deploymentRoot, final String descriptorName) {
        // Locate the descriptor
        final VirtualFile descriptor;
        // EJB 3.1 FR 20.4 Enterprise Beans Packaged in a .war
        if (isWar(deploymentRoot)) {
            // it's a .war file, so look for the ejb-jar.xml in WEB-INF
            descriptor = deploymentRoot.getChild(WEB_INF + "/" + descriptorName);
        } else if (deploymentRoot.getName().toLowerCase(Locale.ENGLISH).endsWith(JAR_FILE_EXTENSION)) {
            descriptor = deploymentRoot.getChild(META_INF + "/" + descriptorName);
        } else {
            // neither a .jar nor a .war. Return
            return null;
        }

        if (descriptor == null || !descriptor.exists()) {
            // no descriptor found, nothing to do!
            return null;
        }
        return descriptor;
    }

    /**
     * Creates and returns a {@link XMLStreamReader} for the passed {@link VirtualFile ejb-jar.xml}
     *
     * @param stream    The input stream
     * @param ejbJarXml
     * @return
     * @throws DeploymentUnitProcessingException
     *
     */
    private static XMLStreamReader getXMLStreamReader(InputStream stream, VirtualFile ejbJarXml, XMLResolver resolver) throws DeploymentUnitProcessingException {
        try {
            final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            inputFactory.setXMLResolver(resolver);
            XMLStreamReader xmlReader = inputFactory.createXMLStreamReader(stream);
            return xmlReader;
        } catch (XMLStreamException xmlse) {
            throw EjbLogger.ROOT_LOGGER.failedToParse(xmlse, "ejb-jar.xml: " + ejbJarXml.getPathName());
        }
    }

    private static boolean isWar(final VirtualFile deploymentRoot) {
        // TODO: Is there a better way to do this?
        return deploymentRoot.getName().toLowerCase(Locale.ENGLISH).endsWith(WAR_FILE_EXTENSION);
    }

    private static InputStream open(final VirtualFile file) throws DeploymentUnitProcessingException {
        try {
            return file.openStream();
        } catch (IOException e) {
            throw new DeploymentUnitProcessingException(e);
        }
    }

    private static EjbJarMetaData parseEjbJarXml(final DeploymentUnit deploymentUnit) throws DeploymentUnitProcessingException {
        final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);

        final VirtualFile alternateDescriptor = deploymentRoot.getAttachment(org.jboss.as.ee.structure.Attachments.ALTERNATE_EJB_DEPLOYMENT_DESCRIPTOR);
        //this is a bit tri

        // Locate the descriptor
        final VirtualFile descriptor;
        if (alternateDescriptor != null) {
            descriptor = alternateDescriptor;
        } else {
            descriptor = getDescriptor(deploymentRoot.getRoot(), EJB_JAR_XML);
        }

        if (descriptor == null) {
            // no descriptor found, nothing to do!
            return null;
        }

        // get the XMLStreamReader and parse the descriptor
        MetaDataElementParser.DTDInfo dtdInfo = new MetaDataElementParser.DTDInfo();
        InputStream stream = open(descriptor);
        try {
            XMLStreamReader reader = getXMLStreamReader(stream, descriptor, dtdInfo);
            EjbJarMetaData ejbJarMetaData = EjbJarMetaDataParser.parse(reader, dtdInfo, SpecDescriptorPropertyReplacement.propertyReplacer(deploymentUnit));
            return ejbJarMetaData;
        } catch (XMLStreamException xmlse) {
            throw EjbLogger.ROOT_LOGGER.failedToParse(xmlse, "ejb-jar.xml: " + descriptor.getPathName());
        } finally {
            try {
                stream.close();
            } catch (IOException ioe) {
                EjbLogger.DEPLOYMENT_LOGGER.failToCloseFile(ioe);
            }
        }
    }

    private static EjbJarMetaData parseJBossEjb3Xml(final DeploymentUnit deploymentUnit) throws DeploymentUnitProcessingException {
        final VirtualFile deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();

        // Locate the descriptor
        final VirtualFile descriptor = getDescriptor(deploymentRoot, JBOSS_EJB3_XML);
        if (descriptor == null) {
            // no descriptor found
            //but there may have been an ejb-jar element in jboss-all.xml
            return deploymentUnit.getAttachment(EjbJarJBossAllParser.ATTACHMENT_KEY);
        }

        // get the XMLStreamReader and parse the descriptor
        MetaDataElementParser.DTDInfo dtdInfo = new MetaDataElementParser.DTDInfo();
        InputStream stream = open(descriptor);
        try {
            XMLStreamReader reader = getXMLStreamReader(stream, descriptor, dtdInfo);

            final JBossEjb3MetaDataParser parser = new JBossEjb3MetaDataParser(createJbossEjbJarParsers());

            final EjbJarMetaData ejbJarMetaData = parser.parse(reader, dtdInfo, JBossDescriptorPropertyReplacement.propertyReplacer(deploymentUnit));
            return ejbJarMetaData;
        } catch (XMLStreamException xmlse) {
            throw EjbLogger.ROOT_LOGGER.failedToParse(xmlse, JBOSS_EJB3_XML + ": " + descriptor.getPathName());
        } finally {
            try {
                stream.close();
            } catch (IOException ioe) {
                EjbLogger.DEPLOYMENT_LOGGER.failToCloseFile(ioe);
            }
        }
    }

    static Map<String, AbstractMetaDataParser<?>> createJbossEjbJarParsers() {
        Map<String, AbstractMetaDataParser<?>> parsers = new HashMap<String, AbstractMetaDataParser<?>>();
        for (ClusteringSchema schema : EnumSet.allOf(ClusteringSchema.class)) {
            parsers.put(schema.getNamespaceUri(), new EJBBoundClusteringMetaDataParser(schema));
        }
        parsers.put(EJBBoundSecurityMetaDataParser.LEGACY_NAMESPACE_URI, EJBBoundSecurityMetaDataParser.INSTANCE);
        parsers.put(EJBBoundSecurityMetaDataParser.NAMESPACE_URI_1_0, EJBBoundSecurityMetaDataParser.INSTANCE);
        parsers.put(EJBBoundSecurityMetaDataParser11.NAMESPACE_URI_1_1, EJBBoundSecurityMetaDataParser11.INSTANCE);
        parsers.put(SecurityRoleMetaDataParser.LEGACY_NAMESPACE_URI, SecurityRoleMetaDataParser.INSTANCE);
        parsers.put(SecurityRoleMetaDataParser.NAMESPACE_URI, SecurityRoleMetaDataParser.INSTANCE);
        parsers.put(EJBBoundResourceAdapterBindingMetaDataParser.LEGACY_NAMESPACE_URI, EJBBoundResourceAdapterBindingMetaDataParser.INSTANCE);
        parsers.put(EJBBoundResourceAdapterBindingMetaDataParser.NAMESPACE_URI, EJBBoundResourceAdapterBindingMetaDataParser.INSTANCE);
        parsers.put(EJBBoundMdbDeliveryMetaDataParser.NAMESPACE_URI_1_0, EJBBoundMdbDeliveryMetaDataParser.INSTANCE);
        parsers.put(EJBBoundMdbDeliveryMetaDataParser11.NAMESPACE_URI_1_1, EJBBoundMdbDeliveryMetaDataParser11.INSTANCE);
        parsers.put("urn:iiop", new IIOPMetaDataParser());
        parsers.put("urn:iiop:1.0", new IIOPMetaDataParser());
        parsers.put("urn:trans-timeout", new TransactionTimeoutMetaDataParser());
        parsers.put("urn:trans-timeout:1.0", new TransactionTimeoutMetaDataParser());
        parsers.put(EJBBoundPoolParser.NAMESPACE_URI, new EJBBoundPoolParser());
        parsers.put(EJBBoundCacheParser.NAMESPACE_URI, new EJBBoundCacheParser());
        parsers.put(ContainerInterceptorsParser.NAMESPACE_URI_1_0, ContainerInterceptorsParser.INSTANCE);
        parsers.put(TimerServiceMetaDataParser.NAMESPACE_URI, TimerServiceMetaDataParser.INSTANCE);
        return parsers;
    }
}
