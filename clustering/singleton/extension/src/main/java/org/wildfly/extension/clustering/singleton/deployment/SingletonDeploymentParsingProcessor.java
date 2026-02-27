/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton.deployment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
import org.jboss.metadata.parser.util.NoopXMLResolver;
import org.jboss.staxmapper.XMLMapper;
import org.jboss.vfs.VirtualFile;
import org.wildfly.common.xml.XMLInputFactoryUtil;

/**
 * Parses a deployment descriptor defining the singleton deployment policy.
 * @author Paul Ferraro
 */
public class SingletonDeploymentParsingProcessor implements DeploymentUnitProcessor {

    private static final String SINGLETON_DEPLOYMENT_DESCRIPTOR = "META-INF/singleton-deployment.xml";
    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactoryUtil.create();
    static {
        XML_INPUT_FACTORY.setXMLResolver(NoopXMLResolver.create());
    }

    private final XMLMapper mapper = XMLElementSchema.createXMLMapper(EnumSet.allOf(SingletonDeploymentSchema.class));

    @Override
    public void deploy(DeploymentPhaseContext context) throws DeploymentUnitProcessingException {
        DeploymentUnit unit = context.getDeploymentUnit();
        if (!unit.hasAttachment(SingletonDeploymentDependencyProcessor.CONFIGURATION_KEY)) {
            VirtualFile file = unit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot().getChild(SINGLETON_DEPLOYMENT_DESCRIPTOR);
            if (file.exists()) {
                try {
                    unit.putAttachment(SingletonDeploymentDependencyProcessor.CONFIGURATION_KEY, this.parse(unit, file.getPhysicalFile()));
                } catch (IOException e) {
                    throw new DeploymentUnitProcessingException(e);
                }
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit unit) {
        unit.removeAttachment(SingletonDeploymentDependencyProcessor.CONFIGURATION_KEY);
    }

    private SingletonDeploymentConfiguration parse(DeploymentUnit unit, File file) throws DeploymentUnitProcessingException {
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            XMLStreamReader xmlReader = XML_INPUT_FACTORY.createXMLStreamReader(reader);
            try  {
                MutableSingletonDeploymentConfiguration config = new MutableSingletonDeploymentConfiguration(unit);
                this.mapper.parseDocument(config, xmlReader);
                return config;
            } finally {
                xmlReader.close();
            }
        } catch (XMLStreamException e) {
            throw ServerLogger.ROOT_LOGGER.errorLoadingDeploymentStructureFile(file.getPath(), e);
        } catch (FileNotFoundException e) {
            throw ServerLogger.ROOT_LOGGER.deploymentStructureFileNotFound(file);
        } catch (IOException e) {
            throw ServerLogger.ROOT_LOGGER.deploymentStructureFileNotFound(file);
        }
    }
}
