/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.structure;

import java.io.InputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.ee.component.DeploymentDescriptorEnvironment;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.metadata.ear.jboss.JBossAppMetaData;
import org.jboss.metadata.ear.merge.JBossAppMetaDataMerger;
import org.jboss.metadata.ear.parser.jboss.JBossAppMetaDataParser;
import org.jboss.metadata.ear.parser.spec.EarMetaDataParser;
import org.jboss.metadata.ear.spec.EarMetaData;
import org.jboss.metadata.parser.util.NoopXMLResolver;
import org.jboss.metadata.property.PropertyReplacer;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;
import org.wildfly.common.xml.XMLInputFactoryUtil;

/**
 * Deployment processor responsible for parsing the application.xml file of an ear.
 *
 * @author John Bailey
 */
public class EarMetaDataParsingProcessor implements DeploymentUnitProcessor {
    private static final String APPLICATION_XML = "META-INF/application.xml";
    private static final String JBOSS_APP_XML = "META-INF/jboss-app.xml";

    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (!DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            return;
        }

        final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.DEPLOYMENT_ROOT);
        final VirtualFile deploymentFile = deploymentRoot.getRoot();

        EarMetaData earMetaData = handleSpecMetadata(deploymentFile, SpecDescriptorPropertyReplacement.propertyReplacer(deploymentUnit));
        JBossAppMetaData jbossMetaData = handleJbossMetadata(deploymentFile, JBossDescriptorPropertyReplacement.propertyReplacer(deploymentUnit), deploymentUnit);
        if (earMetaData == null && jbossMetaData == null) {
            return;
        }
        // the jboss-app.xml has a distinct-name configured then attach it to the deployment unit
        if (jbossMetaData != null && jbossMetaData.getDistinctName() != null) {
            deploymentUnit.putAttachment(Attachments.DISTINCT_NAME, jbossMetaData.getDistinctName());
        }
        JBossAppMetaData merged;
        if (earMetaData != null) {
            merged = new JBossAppMetaData(earMetaData.getEarVersion());
        } else {
            merged = new JBossAppMetaData();
        }
        JBossAppMetaDataMerger.merge(merged, jbossMetaData, earMetaData);

        deploymentUnit.putAttachment(Attachments.EAR_METADATA, merged);
        if (merged.getEarEnvironmentRefsGroup() != null) {
            final DeploymentDescriptorEnvironment bindings = new DeploymentDescriptorEnvironment("java:app/env/", merged.getEarEnvironmentRefsGroup());
            deploymentUnit.putAttachment(org.jboss.as.ee.component.Attachments.MODULE_DEPLOYMENT_DESCRIPTOR_ENVIRONMENT, bindings);
        }

    }

    private EarMetaData handleSpecMetadata(VirtualFile deploymentFile, final PropertyReplacer propertyReplacer) throws DeploymentUnitProcessingException {
        final VirtualFile applicationXmlFile = deploymentFile.getChild(APPLICATION_XML);
        if (!applicationXmlFile.exists()) {
            return null;
        }
        InputStream inputStream = null;
        try {
            inputStream = applicationXmlFile.openStream();
            final XMLInputFactory inputFactory = XMLInputFactoryUtil.create();
            inputFactory.setXMLResolver(NoopXMLResolver.create());
            XMLStreamReader xmlReader = inputFactory.createXMLStreamReader(inputStream);
            return EarMetaDataParser.INSTANCE.parse(xmlReader, propertyReplacer);

        } catch (Exception e) {
            throw EeLogger.ROOT_LOGGER.failedToParse(e, applicationXmlFile);
        } finally {
            VFSUtils.safeClose(inputStream);
        }
    }


    private JBossAppMetaData handleJbossMetadata(VirtualFile deploymentFile, final PropertyReplacer propertyReplacer, final DeploymentUnit deploymentUnit) throws DeploymentUnitProcessingException {
        final VirtualFile applicationXmlFile = deploymentFile.getChild(JBOSS_APP_XML);
        if (!applicationXmlFile.exists()) {
            //may have been in jboss-all.xml
            return deploymentUnit.getAttachment(AppJBossAllParser.ATTACHMENT_KEY);
        }
        InputStream inputStream = null;
        try {
            inputStream = applicationXmlFile.openStream();
            final XMLInputFactory inputFactory = XMLInputFactoryUtil.create();
            inputFactory.setXMLResolver(NoopXMLResolver.create());
            XMLStreamReader xmlReader = inputFactory.createXMLStreamReader(inputStream);
            return JBossAppMetaDataParser.INSTANCE.parse(xmlReader, propertyReplacer);

        } catch (Exception e) {
            throw EeLogger.ROOT_LOGGER.failedToParse(e, applicationXmlFile);
        } finally {
            VFSUtils.safeClose(inputStream);
        }
    }

    public void undeploy(final DeploymentUnit deploymentUnit) {
        deploymentUnit.removeAttachment(Attachments.EAR_METADATA);
    }


}
