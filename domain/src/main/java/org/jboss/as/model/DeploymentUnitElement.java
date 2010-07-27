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

package org.jboss.as.model;

import org.jboss.as.deployment.DeploymentException;
import org.jboss.as.deployment.DeploymentResult;
import org.jboss.as.deployment.DeploymentResultImpl;
import org.jboss.as.deployment.DeploymentService;
import org.jboss.as.deployment.DeploymentServiceListener;
import org.jboss.as.deployment.chain.DeploymentChainProvider;
import org.jboss.as.deployment.chain.DeploymentChainProviderTranslator;
import org.jboss.as.deployment.item.DeploymentItem;
import org.jboss.as.deployment.item.DeploymentItemContext;
import org.jboss.as.deployment.item.DeploymentItemContextImpl;
import org.jboss.as.deployment.module.DeploymentModuleLoader;
import org.jboss.as.deployment.module.DeploymentModuleLoaderProvider;
import org.jboss.as.deployment.module.DeploymentModuleLoaderProviderTranslator;
import org.jboss.as.deployment.module.DeploymentModuleLoaderSelector;
import org.jboss.as.deployment.module.ModuleConfig;
import org.jboss.as.deployment.chain.DeploymentChain;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitContextImpl;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.msc.inject.PropertyInjector;
import org.jboss.msc.inject.TranslatingInjector;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.Location;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.StartException;
import org.jboss.msc.services.VFSMountService;
import org.jboss.msc.value.Values;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;

import javax.xml.stream.XMLStreamException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static org.jboss.as.deployment.attachment.VirtualFileAttachment.attachVirtualFile;

/**
 * A deployment that is known to the domain.
 * 
 * @author Brian Stansberry
 */
public final class DeploymentUnitElement extends AbstractModelElement<DeploymentUnitElement> {

    private static final long serialVersionUID = 5335163070198512362L;
    private static final ServiceName MOUNT_SERVICE_NAME = ServiceName.JBOSS.append("mounts");

    private final DeploymentUnitKey key;
    private boolean allowed;
    private boolean start;

    public DeploymentUnitElement(final Location location, final String fileName, 
            final byte[] sha1Hash, final boolean allowed, final boolean start) {
        super(location);
        this.key = new DeploymentUnitKey(fileName, sha1Hash);
        this.allowed = allowed;
        this.start = start;
    }
    
    public DeploymentUnitElement(XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader);
        // Handle attributes
        String fileName = null;
        String sha1Hash = null;
        String allowed = null;
        String start = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        fileName = value;
                        break;
                    }
                    case SHA1: {
                        sha1Hash = value;
                        break;
                    }
                    case ALLOWED: {
                        allowed = value;
                        break;
                    }
                    case START: {
                        start = value;
                        break;
                    }
                    default: throw unexpectedAttribute(reader, i);
                }
            }
        }
        if (fileName == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
        }
        if (sha1Hash == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.SHA1));
        }
        this.key = new DeploymentUnitKey(fileName, sha1Hash.getBytes());
        this.allowed = allowed == null ? true : Boolean.valueOf(allowed);
        this.start = start == null ? true : Boolean.valueOf(start);
        
        // Handle elements
        requireNoContent(reader);
    }
    
    /**
     * Gets the identifier of this deployment that's suitable for use as a map key.
     * @return the key
     */
    public DeploymentUnitKey getKey() {
        return key;
    }
    
    /**
     * Gets the name of the deployment.
     * 
     * @return the name
     */
    public String getName() {
        return key.getName();
    }

    /**
     * Gets a defensive copy of the sha1 hash of the deployment.
     * 
     * @return the hash
     */
    public byte[] getSha1Hash() {
        return key.getSha1Hash();
    }

    /**
     * Gets whether the deployment should be started upon server start.
     * 
     * @return <code>true</code> if the deployment should be started; <code>false</code>
     *         if not.
     */
    public boolean isStart() {
        return start;
    }
    
    /**
     * Sets whether the deployments should be started upon server start.
     * @param start <code>true</code> if the deployment should be started; <code>false</code>
     *         if not.
     */
    void setStart(boolean start) {
        this.start = start;
    }

    /**
     * Gets whether the deployment can be mapped to a server group; i.e. made
     * available to servers.
     * 
     * @return <code>true</code> if the deployment can be mapped; <code>false</code>
     *         if not.
     */
    public boolean isAllowed() {
        return allowed;
    }
    
    /**
     * Sets whether the deployment can be mapped to a server group; i.e. made
     * available to servers.
     * 
     * @param allowed <code>true</code> if the deployment can be mapped; <code>false</code>
     *         if not.
     */
    void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }

    public DeploymentResult.Future activate(final ServiceContainer serviceContainer) throws DeploymentException {
        final DeploymentResultImpl.FutureImpl future = new DeploymentResultImpl.FutureImpl();

        // Setup batch
        final BatchBuilder batchBuilder = serviceContainer.batchBuilder();
        // Setup deployment listener
        final DeploymentServiceListener deploymentServiceListener = new DeploymentServiceListener(new DeploymentServiceListener.Callback() {
            @Override
            public void run(Map<ServiceName, StartException> serviceFailures, long elapsedTime, int numServices) {
                DeploymentResult.Result result = DeploymentResult.Result.SUCCESS;
                DeploymentException deploymentException = null;
                if(serviceFailures.size() > 0) {
                    result = DeploymentResult.Result.FAILURE;
                    deploymentException = new DeploymentException("Failed to execute deployments.  Not all services started cleanly.");
                }
                future.setDeploymentResult(new DeploymentResultImpl(result, deploymentException, serviceFailures, elapsedTime, numServices));
            }
        });
        batchBuilder.addListener(deploymentServiceListener);

        final DeploymentUnitKey key = this.key;

        // HMMMMMMM
        final VirtualFile deploymentRoot = VFS.getChild(key.getName());
        if(!deploymentRoot.exists())
            throw new DeploymentException("Deployment root does not exist." + deploymentRoot);

        // Create the deployment unit context
        final String deploymentName = key.getName() + ":" + key.getSha1HashAsHexString();
        final DeploymentUnitContextImpl deploymentUnitContext = new DeploymentUnitContextImpl(deploymentName);
        attachVirtualFile(deploymentUnitContext, deploymentRoot);

        // Setup VFS mount service
        // TODO: We should make sure this is an archive first...
        final ServiceName mountServiceName = MOUNT_SERVICE_NAME.append(deploymentName);
        final VFSMountService vfsMountService = new VFSMountService(deploymentRoot.getPathName(), null, false);
        batchBuilder.addService(mountServiceName, vfsMountService)
            .setInitialMode(ServiceController.Mode.ON_DEMAND);

        // Setup deployment service
        final ServiceName deploymentServiceName = DeploymentService.SERVICE_NAME.append(deploymentName);
        final DeploymentService deploymentService = new DeploymentService(deploymentName);
        final BatchServiceBuilder deploymentServiceBuilder = batchBuilder.addService(deploymentServiceName, deploymentService)
            .setInitialMode(start ? ServiceController.Mode.IMMEDIATE : ServiceController.Mode.NEVER);
        deploymentServiceBuilder.addDependency(mountServiceName);
        deploymentServiceBuilder.addDependency(DeploymentChainProvider.SERVICE_NAME)
            .toInjector(new TranslatingInjector<DeploymentChainProvider, DeploymentChain>(
                new DeploymentChainProviderTranslator(deploymentRoot),
                new PropertyInjector(DeploymentService.DEPLOYMENT_CHAIN_PROPERTY, Values.immediateValue(deploymentService)))
            );
        deploymentServiceBuilder.addDependency(DeploymentModuleLoaderProvider.SERVICE_NAME)
            .toInjector(new TranslatingInjector<DeploymentModuleLoaderProvider, DeploymentModuleLoader>(
                new DeploymentModuleLoaderProviderTranslator(deploymentRoot),
                new PropertyInjector(DeploymentService.MODULE_LOADER_PROPERTY, Values.immediateValue(deploymentService)))
            );

        // Setup the listener for a new batch
        deploymentServiceListener.startBatch(new Runnable() {
            public void run() {
                try {
                    if(deploymentService.getDeploymentChain() == null) throw new DeploymentException("Unable to determine deployment chain for deployment: " + deploymentName);
                    if(deploymentService.getModuleLoader() == null) throw new DeploymentException("Unable to determine deployment module loader for deployment: " + deploymentName);

                    executeDeploymentProcessors(deploymentRoot, deploymentUnitContext, deploymentService);
                } catch(DeploymentException e) {
                    future.setDeploymentResult(new DeploymentResultImpl(DeploymentResult.Result.FAILURE, e, Collections.<ServiceName, StartException>emptyMap(), 0L, 0));
                    return;
                }
                try {
                    executeDeploymentItems(future, serviceContainer, deploymentRoot, deploymentUnitContext, deploymentServiceName, deploymentService, deploymentServiceListener);
                } catch(DeploymentException e) {
                    future.setDeploymentResult(new DeploymentResultImpl(DeploymentResult.Result.FAILURE, e, Collections.<ServiceName, StartException>emptyMap(), 0L, 0));
                }
            }
        });

        // Install the batch.
        try {
            batchBuilder.install();
            deploymentServiceListener.finishBatch();
        } catch(ServiceRegistryException e) {
            throw new DeploymentException(e);
        }
        return future;
    }

    /**
     * Phase 2 - Execute deployment processors
     */
    private void executeDeploymentProcessors(final VirtualFile deploymentRoot, final DeploymentUnitContext deploymentUnitContext, final DeploymentService deploymentService) throws DeploymentException {
        // Determine which deployment chain to use for this deployment
        final DeploymentChain deploymentChain = deploymentService.getDeploymentChain();

        // Determine which deployment module loader to use for this deployment
        final DeploymentModuleLoader deploymentModuleLoader = deploymentService.getModuleLoader();
        DeploymentModuleLoaderSelector.CURRENT_MODULE_LOADER.set(deploymentModuleLoader);

        // Execute the deployment chain
        try {
            deploymentChain.processDeployment(deploymentUnitContext);
        } catch(DeploymentUnitProcessingException e) {
            throw new DeploymentException("Failed to process deployment chain.", e);
        } finally {
            DeploymentModuleLoaderSelector.CURRENT_MODULE_LOADER.set(null);
        }
    }

    /**
     * Phase 3 - Create the module and execute the deployment items
     */
    private void executeDeploymentItems(final DeploymentResultImpl.FutureImpl future, final ServiceContainer serviceContainer, final VirtualFile deploymentRoot, final DeploymentUnitContextImpl deploymentUnitContext, final ServiceName deploymentServiceName, final DeploymentService deploymentService, final DeploymentServiceListener deploymentServiceListener) throws DeploymentException {
        // Setup batch
        final BatchBuilder batchBuilder = serviceContainer.batchBuilder();
        batchBuilder.addListener(deploymentServiceListener);

        // Setup the listener for a new batch
        deploymentServiceListener.startBatch();

        // Setup deployment module
        // Determine which deployment module loader to use for this deployment
        final DeploymentModuleLoader deploymentModuleLoader = deploymentService.getModuleLoader();
        Module module = null;
        final ModuleConfig moduleConfig = deploymentUnitContext.getAttachment(ModuleConfig.ATTACHMENT_KEY);
        if(moduleConfig != null) {
            try {
                module = deploymentModuleLoader.loadModule(moduleConfig.getIdentifier());
            } catch(ModuleLoadException e) {
                throw new DeploymentException("Failed to load deployment module.  The module spec was likely not added to the deployment module loader", e);
            }
        }

        // Create a sub batch for the deployment
        final BatchBuilder subBatchBuilder = batchBuilder.subBatchBuilder();

        // Install dependency on the deployment service
        subBatchBuilder.addDependency(deploymentServiceName);

        final ClassLoader currentCl = getContextClassLoader();
        try {
            if(module != null) {
                setContextClassLoader(module.getClassLoader());
            }
            DeploymentModuleLoaderSelector.CURRENT_MODULE_LOADER.set(deploymentModuleLoader);

            // Construct an item context
            final DeploymentItemContext deploymentItemContext = new DeploymentItemContextImpl(module, subBatchBuilder);

            // Process all the deployment items with the item context
            final Collection<DeploymentItem> deploymentItems = deploymentUnitContext.getDeploymentItems();
            for(DeploymentItem deploymentItem : deploymentItems) {
                deploymentItem.install(deploymentItemContext);
            }
        } finally {
            setContextClassLoader(currentCl);
            DeploymentModuleLoaderSelector.CURRENT_MODULE_LOADER.set(null);
        }

        // Install the batch.
        try {
            batchBuilder.install();
            deploymentServiceListener.finishBatch();
            deploymentServiceListener.finishDeployment();
        } catch(ServiceRegistryException e) {
            throw new DeploymentException(e);
        }
    }

    private ClassLoader getContextClassLoader() {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            public ClassLoader run() {
                return Thread.currentThread().getContextClassLoader();
            }
        });
    }

    private void setContextClassLoader(final ClassLoader classLoader) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                Thread.currentThread().setContextClassLoader(classLoader);
                return null;
            }
        });
    }

    public long elementHash() {
        long hash = key.elementHash();
        hash = Long.rotateLeft(hash, 1) ^ Boolean.valueOf(start).hashCode() & 0xffffffffL;
        hash = Long.rotateLeft(hash, 1) ^ Boolean.valueOf(allowed).hashCode() & 0xffffffffL;
        return hash;
    }

    protected void appendDifference(final Collection<AbstractModelUpdate<DeploymentUnitElement>> target, final DeploymentUnitElement other) {
        // FIXME implement appendDifference
        throw new UnsupportedOperationException("implement me");
    }

    protected Class<DeploymentUnitElement> getElementClass() {
        return DeploymentUnitElement.class;
    }

    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.NAME.getLocalName(), key.getName());
        streamWriter.writeAttribute(Attribute.SHA1.getLocalName(), key.getSha1HashAsHexString());
        streamWriter.writeEndElement();
    }
}
