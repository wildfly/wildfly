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

package org.wildfly.extension.clustering.singleton.deployment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.logging.ServerLogger;
import org.jboss.staxmapper.XMLMapper;
import org.jboss.vfs.VirtualFile;

/**
 * Parses a deployment descriptor defining the singleton deployment policy.
 * @author Paul Ferraro
 */
public class SingletonDeploymentParsingProcessor implements DeploymentUnitProcessor {

    private static final String SINGLETON_DEPLOYMENT_DESCRIPTOR = "META-INF/singleton-deployment.xml";
    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();

    private final XMLMapper mapper = XMLMapper.Factory.create();

    public SingletonDeploymentParsingProcessor() {
        for (SingletonDeploymentSchema schema : SingletonDeploymentSchema.values()) {
            this.mapper.registerRootElement(schema.getRoot(), new SingletonDeploymentXMLReader(schema));
        }
    }

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
        try (FileReader reader = new FileReader(file)) {
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
