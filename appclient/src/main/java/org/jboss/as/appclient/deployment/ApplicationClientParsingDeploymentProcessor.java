/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.appclient.deployment;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.appclient.logging.AppClientLogger;
import org.jboss.as.ee.component.DeploymentDescriptorEnvironment;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.metadata.MetadataCompleteMarker;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.ee.structure.JBossDescriptorPropertyReplacement;
import org.jboss.as.ee.structure.SpecDescriptorPropertyReplacement;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.metadata.appclient.jboss.JBossClientMetaData;
import org.jboss.metadata.appclient.parser.jboss.JBossClientMetaDataParser;
import org.jboss.metadata.appclient.parser.spec.ApplicationClientMetaDataParser;
import org.jboss.metadata.appclient.spec.AppClientEnvironmentRefsGroupMetaData;
import org.jboss.metadata.appclient.spec.ApplicationClientMetaData;
import org.jboss.metadata.parser.util.NoopXMLResolver;
import org.jboss.metadata.property.PropertyReplacer;
import org.jboss.vfs.VirtualFile;
import org.wildfly.common.xml.XMLInputFactoryUtil;

/**
 * @author Stuart Douglas
 */
public class ApplicationClientParsingDeploymentProcessor implements DeploymentUnitProcessor {

    private static final String APP_XML = "META-INF/application-client.xml";
    private static final String JBOSS_CLIENT_XML = "META-INF/jboss-client.xml";

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (!DeploymentTypeMarker.isType(DeploymentType.APPLICATION_CLIENT, deploymentUnit)) {
            return;
        }

        final ApplicationClientMetaData appClientMD = parseAppClient(deploymentUnit, SpecDescriptorPropertyReplacement.propertyReplacer(deploymentUnit));
        final JBossClientMetaData jbossClientMD = parseJBossClient(deploymentUnit, JBossDescriptorPropertyReplacement.propertyReplacer(deploymentUnit));
        final JBossClientMetaData merged;
        if (appClientMD == null && jbossClientMD == null) {
            return;
        } else if (appClientMD == null) {
            merged = jbossClientMD;
        } else {
            merged = new JBossClientMetaData();
            merged.setEnvironmentRefsGroupMetaData(new AppClientEnvironmentRefsGroupMetaData());
            merged.merge(jbossClientMD, appClientMD);
        }
        if(merged.isMetadataComplete()) {
            MetadataCompleteMarker.setMetadataComplete(deploymentUnit, true);
        }
        deploymentUnit.putAttachment(AppClientAttachments.APPLICATION_CLIENT_META_DATA, merged);
        final DeploymentDescriptorEnvironment environment = new DeploymentDescriptorEnvironment("java:module/env/", merged.getEnvironmentRefsGroupMetaData());
        deploymentUnit.putAttachment(org.jboss.as.ee.component.Attachments.MODULE_DEPLOYMENT_DESCRIPTOR_ENVIRONMENT, environment);


        //override module name if applicable
        if(merged.getModuleName() != null && !merged.getModuleName().isEmpty()) {
            final EEModuleDescription description = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
            description.setModuleName(merged.getModuleName());
        }

    }

    private ApplicationClientMetaData parseAppClient(DeploymentUnit deploymentUnit, final PropertyReplacer propertyReplacer) throws DeploymentUnitProcessingException {
        final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        final VirtualFile alternateDescriptor = deploymentRoot.getAttachment(org.jboss.as.ee.structure.Attachments.ALTERNATE_CLIENT_DEPLOYMENT_DESCRIPTOR);
        // Locate the descriptor
        final VirtualFile descriptor;
        if (alternateDescriptor != null) {
            descriptor = alternateDescriptor;
        } else {
            descriptor = deploymentRoot.getRoot().getChild(APP_XML);
        }
        if (descriptor.exists()) {
            try (InputStream is = descriptor.openStream()) {
                return new ApplicationClientMetaDataParser().parse(getXMLStreamReader(is), propertyReplacer);
            } catch (XMLStreamException e) {
                throw AppClientLogger.ROOT_LOGGER.failedToParseXml(e, descriptor, e.getLocation().getLineNumber(), e.getLocation().getColumnNumber());
            } catch (IOException e) {
                throw AppClientLogger.ROOT_LOGGER.failedToParseXml(e, descriptor);
            }
        } else {
            return null;
        }
    }

    private JBossClientMetaData parseJBossClient(DeploymentUnit deploymentUnit, final PropertyReplacer propertyReplacer) throws DeploymentUnitProcessingException {
        final VirtualFile deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
        final VirtualFile appXml = deploymentRoot.getChild(JBOSS_CLIENT_XML);
        if (appXml.exists()) {
            try (InputStream is = appXml.openStream()) {
                return new JBossClientMetaDataParser().parse(getXMLStreamReader(is), propertyReplacer);
            } catch (XMLStreamException e) {
                throw AppClientLogger.ROOT_LOGGER.failedToParseXml(e, appXml, e.getLocation().getLineNumber(), e.getLocation().getColumnNumber());

            } catch (IOException e) {
                throw AppClientLogger.ROOT_LOGGER.failedToParseXml(e, appXml);
            }
        } else {
            //we may already have this info from jboss-all.xml
            return deploymentUnit.getAttachment(AppClientJBossAllParser.ATTACHMENT_KEY);
        }
    }

    private XMLStreamReader getXMLStreamReader(InputStream is) throws XMLStreamException {
        final XMLInputFactory inputFactory = XMLInputFactoryUtil.create();
        inputFactory.setXMLResolver(NoopXMLResolver.create());
        return inputFactory.createXMLStreamReader(is);
    }
}
