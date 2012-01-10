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

package org.jboss.as.management.client.content;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.HashUtil;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.deployment.repository.api.ContentRepository;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.vfs.VirtualFile;

/**
 * {@link Resource} implementation for the root resource of a tree of resources that store managed DMR content
 * (e.g. named rollout plans.)
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ManagedDMRContentTypeResource implements Resource.ResourceEntry {

    private final PathElement pathElement;
    private final String childType;
    private final ContentRepository contentRepository;
    private final Map<String, ManagedContent> content = new HashMap<String, ManagedContent>();
    private final ModelNode model = new ModelNode();
    private final MessageDigest messageDigest;

    public ManagedDMRContentTypeResource(final PathElement pathElement, final String childType,
                                         final byte[] initialHash, final ContentRepository contentRepository) {
        this.pathElement = pathElement;
        this.childType = childType;
        this.contentRepository = contentRepository;

        try {
            this.messageDigest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw ManagedDMRContentMessages.MESSAGES.messageDigestAlgorithmNotAvailable(e);
        }

        // Establish an initial hash attribute
        model.get(ModelDescriptionConstants.HASH);

        if (initialHash != null) {
            loadContent(initialHash);
        } // else leave attribute undefined
    }

    private ManagedDMRContentTypeResource(final ManagedDMRContentTypeResource toCopy) {
        this.pathElement = toCopy.pathElement;
        this.childType = toCopy.childType;
        this.contentRepository = toCopy.contentRepository;
        this.messageDigest = toCopy.messageDigest;
        synchronized (toCopy.content) {
            for (Map.Entry<String, ManagedContent> entry : toCopy.content.entrySet()) {
                ManagedContent value = entry.getValue();
                this.content.put(entry.getKey(), new ManagedContent(value.getContent(), value.getHash()));
            }
        }
        this.model.set(toCopy.model);
    }

    @Override
    public ModelNode getModel() {
        return model.clone();
    }

    @Override
    public void writeModel(ModelNode newModel) {
        if (model.hasDefined(ModelDescriptionConstants.HASH)) {
            throw MESSAGES.immutableResource();
        } else {
            // ApplyRemoteMasterDomainModelHandler is writing us
            byte[] initialHash = newModel.hasDefined(ModelDescriptionConstants.HASH) ? newModel.get(ModelDescriptionConstants.HASH).asBytes() : null;
            if (initialHash != null) {
                loadContent(initialHash);
            }
        }
    }

    @Override
    public boolean isModelDefined() {
        return true;
    }

    @Override
    public Resource getChild(PathElement element) {
        if (hasChildren(element.getKey())) {
            synchronized (content) {
                String name = element.getValue();
                ManagedContent managedContent = content.get(name);
                if (managedContent != null) {
                    return getChildEntry(name);
                }
            }
        }
        return null;
    }

    @Override
    public Resource requireChild(PathElement address) {
        final Resource resource = getChild(address);
        if (resource == null) {
            throw new NoSuchResourceException(address);
        }
        return resource;
    }

    @Override
    public Resource navigate(PathAddress address) {
        if (address.size() == 0) {
            return this;
        } else {
            Resource child = requireChild(address.getElement(0));
            return address.size() == 1 ? child : child.navigate(address.subAddress(1));
        }
    }

    @Override
    public Set<String> getChildTypes() {
        return Collections.singleton(childType);
    }

    @Override
    public Set<Resource.ResourceEntry> getChildren(String childType) {
        if (!hasChildren(childType)) {
            return Collections.emptySet();
        } else {
            Set<Resource.ResourceEntry> result = new HashSet<ResourceEntry>();
            synchronized (content) {
                for (String name : content.keySet()) {
                    result.add(getChildEntry(name));
                }
            }
            return result;
        }
    }

    @Override
    public final boolean hasChildren(String childType) {
        return this.childType.equals(childType);
    }

    @Override
    public boolean hasChild(PathElement element) {
        return getChildrenNames(element.getKey()).contains(element.getValue());
    }

    @Override
    public Set<String> getChildrenNames(String childType) {
        if (hasChildren(childType)) {
            synchronized (content) {
                return new HashSet<String>(content.keySet());
            }
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public void registerChild(PathElement address, Resource resource) {
        if (!childType.equals(address.getKey())) {
            throw ManagedDMRContentMessages.MESSAGES.illegalChildType(address.getKey(), childType);
        }
        if (! (resource instanceof ManagedDMRContentResource)) {
            throw ManagedDMRContentMessages.MESSAGES.illegalChildClass(resource.getClass());
        }

        // Just attach ourself to this child so during the course of this operation it can access data
        ManagedDMRContentResource child = ManagedDMRContentResource.class.cast(resource);
        child.setParent(this);
    }

    @Override
    public Resource removeChild(PathElement address) {
        final Resource toRemove = getChild(address);
        if (toRemove != null) {
            synchronized (content) {
                content.remove(address.getValue());
            }
            try{
                storeContent();
            } catch (IOException e) {
                throw new ContentStorageException(e);
            }
        }
        return toRemove;
    }

    @Override
    public boolean isRuntime() {
        return false;
    }

    @Override
    public boolean isProxy() {
        return false;
    }

    @SuppressWarnings({"CloneDoesntCallSuperClone"})
    @Override
    public Resource clone() {
        return new ManagedDMRContentTypeResource(this);
    }

    @Override
    public String getName() {
        return this.pathElement.getValue();
    }

    @Override
    public PathElement getPathElement() {
        return this.pathElement;
    }

    ManagedContent getManagedContent(final String name) {
        return content.get(name);
    }

    byte[] storeManagedContent(final String name, final ModelNode content) throws IOException {
        final byte[] hash = hashContent(content);
        synchronized (this.content) {
            this.content.put(name, new ManagedContent(content, hash));
        }
        storeContent();
        return hash;
    }

    private void loadContent(byte[] initialHash) {
        VirtualFile vf = contentRepository.getContent(initialHash);
        if (vf == null) {
            throw ManagedDMRContentMessages.MESSAGES.noContentFoundWithHash(HashUtil.bytesToHexString(initialHash));
        }
        InputStream is = null;
        try {
            is = vf.openStream();
            ModelNode node = ModelNode.fromStream(is);
            if (node.isDefined()) {
                for (Property prop : node.asPropertyList()) {
                    ModelNode value = prop.getValue();
                    byte[] hash = hashContent(value);
                    synchronized (content) {
                        content.put(prop.getName(), new ManagedContent(value, hash));
                    }
                }
            }
            this.model.get(ModelDescriptionConstants.HASH).set(initialHash);
        } catch (IOException e) {
            throw new ContentStorageException(e);
        } finally {
            safeClose(is);
        }
    }

    private void storeContent() throws IOException {
        final ModelNode node = new ModelNode();
        boolean hasContent;
        synchronized (content) {
            hasContent = !content.isEmpty();
            if (hasContent) {
                for (Map.Entry<String, ManagedContent> entry : content.entrySet()) {
                    node.get(entry.getKey()).set(entry.getValue().content);
                }
            }
        }
        if (hasContent) {
            ByteArrayInputStream bais = new ByteArrayInputStream(node.toString().getBytes());
            byte[] ourHash = contentRepository.addContent(bais);
            this.model.get(ModelDescriptionConstants.HASH).set(ourHash);
        } else {
            this.model.get(ModelDescriptionConstants.HASH).clear();
        }
    }

    private byte[] hashContent(ModelNode content) throws IOException {
        byte[] sha1Bytes;
        OutputStream os = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                // just discard
            }
        };
        synchronized (messageDigest) {
            messageDigest.reset();

            DigestOutputStream dos = new DigestOutputStream(os, messageDigest);
            ByteArrayInputStream bis = new ByteArrayInputStream(content.toString().getBytes());
            byte[] bytes = new byte[8192];
            int read;
            while ((read = bis.read(bytes)) > -1) {
                dos.write(bytes, 0, read);
            }

            sha1Bytes = messageDigest.digest();
        }
        return sha1Bytes;
    }

    private ResourceEntry getChildEntry(String name) {
        return new ManagedDMRContentResource(PathElement.pathElement(childType, name) ,this);
    }

    private static void safeClose(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    static class ManagedContent {
        private final ModelNode content;
        private final byte[] hash;

        ManagedContent(ModelNode content, byte[] hash) {
            this.content = content;
            this.hash = hash;
        }

        ModelNode getContent() {
            return content.clone();
        }

        byte[] getHash() {
            return Arrays.copyOf(hash, hash.length);
        }
    }
}
