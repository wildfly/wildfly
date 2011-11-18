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
package org.jboss.as.webservices.deployers.deployment;

import static org.jboss.as.webservices.WSLogger.ROOT_LOGGER;
import static org.jboss.as.webservices.WSMessages.MESSAGES;
import static org.jboss.as.webservices.util.ASHelper.getJBossWebMetaData;
import static org.jboss.as.webservices.util.ASHelper.getOptionalAttachment;
import static org.jboss.as.webservices.util.WSAttachmentKeys.CLASSLOADER_KEY;
import static org.jboss.as.webservices.util.WSAttachmentKeys.DEPLOYMENT_KEY;
import static org.jboss.as.webservices.util.WSAttachmentKeys.JAXRPC_ENDPOINTS_KEY;
import static org.jboss.as.webservices.util.WSAttachmentKeys.JAXWS_ENDPOINTS_KEY;
import static org.jboss.as.webservices.util.WSAttachmentKeys.JBOSS_WEBSERVICES_METADATA_KEY;
import static org.jboss.as.webservices.util.WSAttachmentKeys.WEBSERVICES_METADATA_KEY;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.webservices.metadata.model.JAXRPCDeployment;
import org.jboss.as.webservices.metadata.model.JAXWSDeployment;
import org.jboss.as.webservices.util.VirtualFileAdaptor;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.modules.Module;
import org.jboss.vfs.VirtualFile;
import org.jboss.ws.common.ResourceLoaderAdapter;
import org.jboss.wsf.spi.SPIProvider;
import org.jboss.wsf.spi.SPIProviderResolver;
import org.jboss.wsf.spi.deployment.ArchiveDeployment;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.DeploymentModelFactory;
import org.jboss.wsf.spi.deployment.DeploymentType;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.deployment.EndpointType;
import org.jboss.wsf.spi.deployment.UnifiedVirtualFile;
import org.jboss.wsf.spi.metadata.webservices.JBossWebservicesMetaData;
import org.jboss.wsf.spi.metadata.webservices.WebservicesMetaData;

/**
 * Base class for all deployment model builders.
 *
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
abstract class AbstractDeploymentModelBuilder implements DeploymentModelBuilder {
    /** WSDL, XSD and XML files filter. */
    private static final WSVirtualFileFilter WS_FILE_FILTER = new WSVirtualFileFilter();

    /** Deployment model factory. */
    private final DeploymentModelFactory deploymentModelFactory;

    /** Deployment type this builder creates. */
    private final DeploymentType deploymentType;

    /** Endpoint type this builder creates. */
    private final EndpointType endpointType;

    /**
     * Constructor.
     */
    protected AbstractDeploymentModelBuilder(final DeploymentType deploymentType, final EndpointType endpointType) {
        // deployment factory
        final SPIProvider spiProvider = SPIProviderResolver.getInstance().getProvider();
        this.deploymentModelFactory = spiProvider.getSPI(DeploymentModelFactory.class);
        this.deploymentType = deploymentType;
        this.endpointType = endpointType;
    }

    /**
     * @see org.jboss.webservices.integration.deployers.deployment.DeploymentModelBuilder#newDeploymentModel(DeploymentUnit)
     *
     * @param unit deployment unit
     */
    public final void newDeploymentModel(final DeploymentUnit unit) {
        final ArchiveDeployment dep;
        if (unit.hasAttachment(DEPLOYMENT_KEY)) {
            dep = (ArchiveDeployment) unit.getAttachment(DEPLOYMENT_KEY);
        } else {
            dep = newDeployment(unit);
            propagateAttachments(unit, dep);
        }

        this.build(dep, unit);
    }

    private void propagateAttachments(final DeploymentUnit unit, final ArchiveDeployment dep) {
        dep.addAttachment(DeploymentUnit.class, unit);
        unit.putAttachment(DEPLOYMENT_KEY, dep);

        final JBossWebMetaData webMD = getJBossWebMetaData(unit);
        dep.addAttachment(JBossWebMetaData.class, webMD);

        final WebservicesMetaData webservicesMD = getOptionalAttachment(unit, WEBSERVICES_METADATA_KEY);
        dep.addAttachment(WebservicesMetaData.class, webservicesMD);

        final JBossWebservicesMetaData jbossWebservicesMD = getOptionalAttachment(unit, JBOSS_WEBSERVICES_METADATA_KEY);
        dep.addAttachment(JBossWebservicesMetaData.class, jbossWebservicesMD);

        final JAXWSDeployment jaxwsDeployment = getOptionalAttachment(unit, JAXWS_ENDPOINTS_KEY);
        dep.addAttachment(JAXWSDeployment.class, jaxwsDeployment);

        final JAXRPCDeployment jaxrpcDeployment = getOptionalAttachment(unit, JAXRPC_ENDPOINTS_KEY);
        dep.addAttachment(JAXRPCDeployment.class, jaxrpcDeployment);

        final EjbJarMetaData ejbJarMD = getOptionalAttachment(unit, EjbDeploymentAttachmentKeys.EJB_JAR_METADATA);
        dep.addAttachment(EjbJarMetaData.class, ejbJarMD);
    }

    /**
     * Template method for subclasses to implement.
     *
     * @param dep webservice deployment
     * @param unit deployment unit
     */
    protected abstract void build(Deployment dep, DeploymentUnit unit);

    /**
     * Creates new Http Web Service endpoint.
     *
     * @param endpointClass endpoint class name
     * @param endpointName endpoint name
     * @param dep deployment
     * @return WS endpoint
     */
    protected final Endpoint newHttpEndpoint(final String endpointClass, final String endpointName, final Deployment dep) {
        if (endpointName == null) throw MESSAGES.nullEndpointName();
        if (endpointClass == null) throw MESSAGES.nullEndpointClass();

        final Endpoint endpoint = this.deploymentModelFactory.newHttpEndpoint(endpointClass);
        endpoint.setShortName(endpointName);
        endpoint.setType(endpointType);
        dep.getService().addEndpoint(endpoint);

        return endpoint;
    }

    /**
     * Creates new JMS Web Service endpoint.
     *
     * @param endpointClass endpoint class name
     * @param endpointName endpoint name
     * @param dep deployment
     * @return WS endpoint
     */
    protected final Endpoint newJMSEndpoint(final String endpointClass, final String endpointName, final String soapAddress, final Deployment dep) {
        if (endpointName == null) throw MESSAGES.nullEndpointName();
        if (endpointClass == null) throw MESSAGES.nullEndpointClass();

        final Endpoint endpoint = deploymentModelFactory.newJMSEndpoint(endpointClass);
        endpoint.setAddress(soapAddress);
        endpoint.setShortName(endpointName);
        endpoint.setType(endpointType);
        dep.getService().addEndpoint(endpoint);

        return endpoint;
    }

    /**
     * Creates new Web Service deployment.
     *
     * @param unit deployment unit
     * @return archive deployment
     */
    private ArchiveDeployment newDeployment(final DeploymentUnit unit) {
        ROOT_LOGGER.creatingUnifiedWebservicesDeploymentModel(unit);
        final ResourceRoot deploymentRoot = unit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        final VirtualFile root = deploymentRoot != null ? deploymentRoot.getRoot() : null;
        final ClassLoader classLoader;
        final Module module = unit.getAttachment(Attachments.MODULE);
        if (module == null) {
            classLoader = unit.getAttachment(CLASSLOADER_KEY);
            if (classLoader == null) {
                throw MESSAGES.classLoaderResolutionFailed(unit);
            }
        } else {
            classLoader = module.getClassLoader();
        }
        final ArchiveDeployment dep = this.newDeployment(unit.getName(), classLoader);

        if (root != null) {
            try {
                List<VirtualFile> virtualFiles = root.getChildrenRecursively(WS_FILE_FILTER);
                final Set<UnifiedVirtualFile> uVirtualFiles = new HashSet<UnifiedVirtualFile>();
                for (VirtualFile vf : virtualFiles) {
                    // Adding the roots of the virtual files.
                    uVirtualFiles.add(new VirtualFileAdaptor(vf));
                }
                dep.setMetadataFiles(new LinkedList<UnifiedVirtualFile>(uVirtualFiles));
            } catch (IOException e) {
                ROOT_LOGGER.cannotLoadMetaDataFiles(e, root);
            }
        }

        if (unit.getParent() != null) {
            final String parentDeploymentName = unit.getParent().getName();
            final Module parentModule = unit.getParent().getAttachment(Attachments.MODULE);
            if (parentModule == null) {
                throw MESSAGES.classLoaderResolutionFailed(deploymentRoot);
            }
            final ClassLoader parentClassLoader = parentModule.getClassLoader();

            ROOT_LOGGER.creatingUnifiedWebservicesDeploymentModel(unit.getParent());
            final ArchiveDeployment parentDep = this.newDeployment(parentDeploymentName, parentClassLoader);
            dep.setParent(parentDep);
        }

        if (root != null) {
            dep.setRootFile(new VirtualFileAdaptor(root));
        } else {
            dep.setRootFile(new ResourceLoaderAdapter(classLoader));
        }
        dep.setRuntimeClassLoader(classLoader);
        dep.setType(deploymentType);

        return dep;
    }

    /**
     * Creates new archive deployment.
     *
     * @param name deployment name
     * @param loader deployment loader
     * @return new archive deployment
     */
    private ArchiveDeployment newDeployment(final String name, final ClassLoader loader) {
        return (ArchiveDeployment) this.deploymentModelFactory.newDeployment(name, loader);
    }
}
