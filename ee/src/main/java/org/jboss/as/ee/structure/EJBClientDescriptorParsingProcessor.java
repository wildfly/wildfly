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

package org.jboss.as.ee.structure;

import org.jboss.as.ee.EeLogger;
import org.jboss.as.ee.EeMessages;
import org.jboss.as.ee.metadata.EJBClientDescriptorMetaData;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.logging.Logger;
import org.jboss.modules.ModuleLoader;
import org.jboss.staxmapper.XMLMapper;
import org.jboss.vfs.VirtualFile;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * A deployment unit processor which parses jboss-ejb-client.xml in top level deployments.
 * If a jboss-ejb-client.xml is found in the top level deployment, then this processor creates a {@link EJBClientDescriptorMetaData}
 * out of it and attaches it to the deployment unit at {@link Attachments#EJB_CLIENT_METADATA}
 *
 * @author Jaikiran Pai
 */
public class EJBClientDescriptorParsingProcessor implements DeploymentUnitProcessor {

    public static final String[] EJB_CLIENT_DESCRIPTOR_LOCATIONS = {"META-INF/jboss-ejb-client.xml", "WEB-INF/jboss-ejb-client.xml"};


    private static final QName ROOT_1_0 = new QName(EJBClientDescriptor10Parser.NAMESPACE_1_0, "jboss-ejb-client");
    private static final QName ROOT_1_1 = new QName(EJBClientDescriptor11Parser.NAMESPACE_1_1, "jboss-ejb-client");
    private static final QName ROOT_1_2 = new QName(EJBClientDescriptor12Parser.NAMESPACE_1_2, "jboss-ejb-client");
    private static final QName ROOT_NO_NAMESPACE = new QName("jboss-ejb-client");


    private static final XMLInputFactory INPUT_FACTORY = XMLInputFactory.newInstance();

    private final XMLMapper mapper;

    public EJBClientDescriptorParsingProcessor() {
        mapper = XMLMapper.Factory.create();
        mapper.registerRootElement(ROOT_1_0, EJBClientDescriptor10Parser.INSTANCE);
        mapper.registerRootElement(ROOT_1_1, EJBClientDescriptor11Parser.INSTANCE);
        mapper.registerRootElement(ROOT_1_2, EJBClientDescriptor12Parser.INSTANCE);
        mapper.registerRootElement(ROOT_NO_NAMESPACE, EJBClientDescriptor12Parser.INSTANCE);
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ResourceRoot resourceRoot = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.DEPLOYMENT_ROOT);
        final ServiceModuleLoader moduleLoader = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.SERVICE_MODULE_LOADER);

        VirtualFile descriptorFile = null;
        for (final String loc : EJB_CLIENT_DESCRIPTOR_LOCATIONS) {
            final VirtualFile file = resourceRoot.getRoot().getChild(loc);
            if (file.exists()) {
                descriptorFile = file;
                break;
            }
        }
        if (descriptorFile == null) {
            return;
        }
        if (deploymentUnit.getParent() != null) {
            EeLogger.ROOT_LOGGER.subdeploymentIgnored(descriptorFile.getPathName());
            return;
        }
        final File ejbClientDeploymentDescriptorFile;
        try {
            ejbClientDeploymentDescriptorFile = descriptorFile.getPhysicalFile();
        } catch (IOException e) {
            throw EeMessages.MESSAGES.failedToProcessEJBClientDescriptor(e);
        }
        final EJBClientDescriptorMetaData ejbClientDescriptorMetaData = parse(ejbClientDeploymentDescriptorFile, deploymentUnit, moduleLoader);
        EeLogger.ROOT_LOGGER.debugf("Successfully parsed jboss-ejb-client.xml for deployment unit %s", deploymentUnit);
        // attach the metadata
        deploymentUnit.putAttachment(Attachments.EJB_CLIENT_METADATA, ejbClientDescriptorMetaData);
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    private EJBClientDescriptorMetaData parse(final File file, final DeploymentUnit deploymentUnit, final ModuleLoader moduleLoader) throws DeploymentUnitProcessingException {
        final FileInputStream fis;
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw EeMessages.MESSAGES.failedToProcessEJBClientDescriptor(e);
        }
        try {
            return parse(fis, file, deploymentUnit, moduleLoader);
        } finally {
            safeClose(fis);
        }
    }

    private EJBClientDescriptorMetaData parse(final InputStream source, final File file, final DeploymentUnit deploymentUnit, final ModuleLoader moduleLoader)
            throws DeploymentUnitProcessingException {
        try {

            final XMLInputFactory inputFactory = INPUT_FACTORY;
            setIfSupported(inputFactory, XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
            setIfSupported(inputFactory, XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
            final XMLStreamReader streamReader = inputFactory.createXMLStreamReader(source);
            try {
                final EJBClientDescriptorMetaData result = new EJBClientDescriptorMetaData();
                mapper.parseDocument(result, streamReader);
                return result;
            } finally {
                safeClose(streamReader);
            }
        } catch (XMLStreamException e) {
            throw EeMessages.MESSAGES.xmlErrorParsingEJBClientDescriptor(e, file.getAbsolutePath());
        }
    }

    private void setIfSupported(final XMLInputFactory inputFactory, final String property, final Object value) {
        if (inputFactory.isPropertySupported(property)) {
            inputFactory.setProperty(property, value);
        }
    }

    private static void safeClose(final Closeable closeable) {
        if (closeable != null)
            try {
                closeable.close();
            } catch (IOException e) {
                // ignore
            }
    }

    private static void safeClose(final XMLStreamReader streamReader) {
        if (streamReader != null)
            try {
                streamReader.close();
            } catch (XMLStreamException e) {
                // ignore
            }
    }
}
