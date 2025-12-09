/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.deployment;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.EnumSet;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.controller.xml.XMLElementSchema;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.logging.ServerLogger;
import org.jboss.staxmapper.XMLMapper;
import org.jboss.vfs.VirtualFile;

/**
 * {@link DeploymentUnitProcessor} that parses a standalone distributable-web deployment descriptor.
 * @author Paul Ferraro
 */
public class DistributableWebDeploymentParsingProcessor implements DeploymentUnitProcessor {

    private static final String DISTRIBUTABLE_WEB_DEPLOYMENT_DESCRIPTOR = "WEB-INF/distributable-web.xml";
    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();

    private final XMLMapper mapper = XMLElementSchema.createXMLMapper(EnumSet.allOf(DistributableWebDeploymentSchema.class));

    @Override
    public void deploy(DeploymentPhaseContext context) throws DeploymentUnitProcessingException {
        DeploymentUnit unit = context.getDeploymentUnit();
        if (!unit.hasAttachment(DistributableWebDeploymentDependencyProcessor.CONFIGURATION_KEY)) {
            VirtualFile file = unit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot().getChild(DISTRIBUTABLE_WEB_DEPLOYMENT_DESCRIPTOR);
            if (file.exists()) {
                try {
                    unit.putAttachment(DistributableWebDeploymentDependencyProcessor.CONFIGURATION_KEY, this.parse(unit, file.getPhysicalFile()));
                } catch (IOException e) {
                    throw new DeploymentUnitProcessingException(e);
                }
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit unit) {
        unit.removeAttachment(DistributableWebDeploymentDependencyProcessor.CONFIGURATION_KEY);
    }

    private DistributableWebDeploymentConfiguration parse(DeploymentUnit unit, File file) throws DeploymentUnitProcessingException {
        try (FileReader reader = new FileReader(file)) {
            XMLStreamReader xmlReader = XML_INPUT_FACTORY.createXMLStreamReader(reader);
            try  {
                MutableDistributableWebDeploymentConfiguration config = new MutableDistributableWebDeploymentConfiguration(unit);
                this.mapper.parseDocument(config, xmlReader);
                return config;
            } finally {
                xmlReader.close();
            }
        } catch (XMLStreamException e) {
            throw ServerLogger.ROOT_LOGGER.errorLoadingDeploymentStructureFile(file.getPath(), e);
        } catch (IOException e) {
            throw ServerLogger.ROOT_LOGGER.deploymentStructureFileNotFound(file);
        }
    }
}
