/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.deployment;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.ee.structure.JBossDescriptorPropertyReplacement;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.metadata.parser.jbossweb.JBossWebMetaDataParser;
import org.jboss.metadata.parser.util.NoopXMLResolver;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.jboss.ValveMetaData;
import org.jboss.vfs.VirtualFile;
import org.wildfly.common.xml.XMLInputFactoryUtil;
import org.wildfly.extension.undertow.logging.UndertowLogger;


/**
 * @author Jean-Frederic Clere
 */
public class JBossWebParsingDeploymentProcessor implements DeploymentUnitProcessor {

    private static final String JBOSS_WEB_XML = "WEB-INF/jboss-web.xml";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (!DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit)) {
            return; // Skip non web deployments
        }
        final VirtualFile deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
        final VirtualFile jbossWebXml = deploymentRoot.getChild(JBOSS_WEB_XML);
        WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        assert warMetaData != null;
        if (jbossWebXml.exists()) {
            InputStream is = null;
            try {
                is = jbossWebXml.openStream();
                final XMLInputFactory inputFactory = XMLInputFactoryUtil.create();
                inputFactory.setXMLResolver(NoopXMLResolver.create());
                XMLStreamReader xmlReader = inputFactory.createXMLStreamReader(is);

                final JBossWebMetaData jBossWebMetaData = JBossWebMetaDataParser.parse(xmlReader, JBossDescriptorPropertyReplacement.propertyReplacer(deploymentUnit));
                warMetaData.setJBossWebMetaData(jBossWebMetaData);
                // if the jboss-web.xml has a distinct-name configured, then attach the value to this
                // deployment unit
                if(jBossWebMetaData.getValves() != null) {
                    for(ValveMetaData valve : jBossWebMetaData.getValves()) {
                        UndertowLogger.ROOT_LOGGER.unsupportedValveFeature(valve.getValveClass());
                    }
                }
                if (jBossWebMetaData.getDistinctName() != null) {
                    deploymentUnit.putAttachment(org.jboss.as.ee.structure.Attachments.DISTINCT_NAME, jBossWebMetaData.getDistinctName());
                }
            } catch (XMLStreamException e) {
                throw new DeploymentUnitProcessingException(UndertowLogger.ROOT_LOGGER.failToParseXMLDescriptor(jbossWebXml.toString(), e.getLocation().getLineNumber(), e.getLocation().getColumnNumber()), e);
            } catch (IOException e) {
                throw new DeploymentUnitProcessingException(UndertowLogger.ROOT_LOGGER.failToParseXMLDescriptor(jbossWebXml.toString()), e);
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    // Ignore
                }
            }
        } else {
            //jboss web embedded inside jboss-all.xml
            final JBossWebMetaData jbMeta = deploymentUnit.getAttachment(WebJBossAllParser.ATTACHMENT_KEY);
            if(jbMeta != null) {
                warMetaData.setJBossWebMetaData(jbMeta);
            }
        }
    }
}
