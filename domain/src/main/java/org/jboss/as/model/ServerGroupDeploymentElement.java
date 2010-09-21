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

import static org.jboss.as.deployment.attachment.VirtualFileAttachment.attachVirtualFile;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.deployment.DeploymentFailureListener;
import org.jboss.as.deployment.DeploymentService;
import org.jboss.as.deployment.chain.DeploymentChain;
import org.jboss.as.deployment.chain.DeploymentChainProvider;
import org.jboss.as.deployment.module.MountHandle;
import org.jboss.as.deployment.module.TempFileProviderService;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitContextImpl;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.logging.Logger;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceName;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;

/**
 * A deployment which is mapped into a {@link ServerGroupElement}.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ServerGroupDeploymentElement extends AbstractModelElement<ServerGroupDeploymentElement> implements ServiceActivator {
    private static final long serialVersionUID = -7282640684801436543L;
    private static final Logger log = Logger.getLogger("org.jboss.as.deployment");

    private final DeploymentUnitKey key;
    private boolean start;

    /**
     * Construct a new instance.
     *
     * @param deploymentName the name of the deployment unit
     * @param deploymentHash the hash of the deployment unit
     */
    public ServerGroupDeploymentElement(final String deploymentName, final byte[] deploymentHash, final boolean start) {
        if (deploymentName == null) {
            throw new IllegalArgumentException("deploymentName is null");
        }
        if (deploymentHash == null) {
            throw new IllegalArgumentException("deploymentHash is null");
        }
        if (deploymentHash.length != 20) {
            throw new IllegalArgumentException("deploymentHash is not a valid length");
        }
        this.key = new DeploymentUnitKey(deploymentName, deploymentHash);
        this.start = start;
    }

    public ServerGroupDeploymentElement(XMLExtendedStreamReader reader) throws XMLStreamException {
        // Handle attributes
        String fileName = null;
        byte[] sha1Hash = null;
        String start = null;
        final int count = reader.getAttributeCount();
        for(int i = 0; i < count; i++) {
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
                        try {
                            sha1Hash = hexStringToByteArray(value);
                        }
                        catch (Exception e) {
                            throw new XMLStreamException("Value " + value +
                                    " for attribute " + attribute.getLocalName() +
                                    " does not represent a properly hex-encoded SHA1 hash",
                                    reader.getLocation(), e);
                        }
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

        this.key = new DeploymentUnitKey(fileName, sha1Hash);
        this.start = start == null ? true : Boolean.valueOf(start);
        // Handle elements
        requireNoContent(reader);

        // TODO:  Read in serialized DIs
    }

    /**
     * Gets the identifier of this deployment that's suitable for use as a map key.
     *
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
     *
     * @param start <code>true</code> if the deployment should be started; <code>false</code>
     *         if not.
     */
    void setStart(boolean start) {
        this.start = start;
    }

    /** {@inheritDoc} */
    public long elementHash() {
        long hash = key.elementHash();
        hash = Long.rotateLeft(hash, 1) ^ Boolean.valueOf(start).hashCode() & 0xffffffffL;
        return hash;
    }

    /** {@inheritDoc} */
    protected void appendDifference(final Collection<AbstractModelUpdate<ServerGroupDeploymentElement>> target, final ServerGroupDeploymentElement other) {
        // FIXME implement appendDifference
        throw new UnsupportedOperationException("implement me");
    }

    /** {@inheritDoc} */
    protected Class<ServerGroupDeploymentElement> getElementClass() {
        return ServerGroupDeploymentElement.class;
    }

    /** {@inheritDoc} */
    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.NAME.getLocalName(), key.getName());
        streamWriter.writeAttribute(Attribute.SHA1.getLocalName(), key.getSha1HashAsHexString());
        if (!this.start) streamWriter.writeAttribute(Attribute.START.getLocalName(), "false");
    }

    @Override
    public void activate(final ServiceActivatorContext context) {
        final String deploymentName = key.getName().replace('.', '_');
        log.infof("Activating deployment: %s", key.getName());

        final VirtualFile deploymentRoot = VFS.getChild(getFullyQualifiedDeploymentPath(key.getName()));
        if (!deploymentRoot.exists())
            throw new RuntimeException("Deployment root does not exist." + deploymentRoot);

        Closeable handle = null;
        try {
            // Mount virtual file
            try {
                if(deploymentRoot.isFile())
                    handle = VFS.mountZip(deploymentRoot, deploymentRoot, TempFileProviderService.provider());
            } catch (IOException e) {
                throw new RuntimeException("Failed to mount deployment archive", e);
            }

            final BatchBuilder batchBuilder = context.getBatchBuilder();
            // Create deployment service
            final ServiceName deploymentServiceName = DeploymentService.SERVICE_NAME.append(deploymentName);
            BatchServiceBuilder<Void> serviceBuilder = batchBuilder.addService(deploymentServiceName, new DeploymentService());

            // Create a sub-batch for this deployment
            final BatchBuilder deploymentSubBatch = batchBuilder.subBatchBuilder();

            // Setup a batch level dependency on deployment service
            deploymentSubBatch.addDependency(deploymentServiceName);

            // Add a deployment failure listener to the batch
            deploymentSubBatch.addListener(new DeploymentFailureListener(deploymentServiceName));

            // Create the deployment unit context
            final DeploymentUnitContext deploymentUnitContext = new DeploymentUnitContextImpl(deploymentName, deploymentSubBatch, serviceBuilder);
            attachVirtualFile(deploymentUnitContext, deploymentRoot);
            deploymentUnitContext.putAttachment(MountHandle.ATTACHMENT_KEY, new MountHandle(handle));

            // Execute the deployment chain
            final DeploymentChainProvider deploymentChainProvider = DeploymentChainProvider.INSTANCE;
            final DeploymentChain deploymentChain = deploymentChainProvider.determineDeploymentChain(deploymentRoot);
            log.debugf("Executing deployment '%s' with chain: %s", key.getName(), deploymentChain);
            if(deploymentChain == null)
                throw new RuntimeException("Failed determine the deployment chain for deployment root: " + deploymentRoot);
            try {
                deploymentChain.processDeployment(deploymentUnitContext);
            } catch (DeploymentUnitProcessingException e) {
                throw new RuntimeException("Failed to process deployment chain.", e);
            }
        } catch(Throwable t) {
            VFSUtils.safeClose(handle);
            throw new RuntimeException("Failed to activate deployment unit " + key.getName(), t);
        }
    }

    private String getFullyQualifiedDeploymentPath(final String fileName) {
        return System.getProperty("jboss.server.deploy.dir") + "/" + fileName;
    }
}
