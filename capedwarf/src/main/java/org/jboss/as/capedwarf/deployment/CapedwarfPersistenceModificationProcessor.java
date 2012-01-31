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

package org.jboss.as.capedwarf.deployment;

import org.jboss.as.jpa.config.Configuration;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.vfs.TempDir;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

/**
 * Fix CapeDwarf persistence.xml usage.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class CapedwarfPersistenceModificationProcessor extends CapedwarfPersistenceProcessor {

    static final String PERSISTENCE_XML = "persistence.xml";
    static final String IN_META_INF = "META-INF/" + PERSISTENCE_XML;
    static final String IN_WEB_INF = "WEB-INF/classes/" + IN_META_INF;

    static final String DATANUCLEUS_PROVIDER = Configuration.PROVIDER_CLASS_DATANUCLEUS;
    static final String DATANUCLEUS_GAE_PROVIDER = Configuration.PROVIDER_CLASS_DATANUCLEUS_GAE;
    static final String PROPERTIES = "<properties>";
    static final String LOAD_AT_RUNTIME = "<property name=\"datanucleus.metadata.allowLoadAtRuntime\" value=\"true\"/>";

    static final AttachmentKey<AttachmentList<Closeable>> ASSEMBLY_HANDLE = AttachmentKey.createList(Closeable.class);
    static final Random rng = new Random();

    final TempDir tempDir;

    public CapedwarfPersistenceModificationProcessor(TempDir tempDir) {
        this.tempDir = tempDir;
    }

    protected void modifyPersistenceInfo(DeploymentUnit unit, ResourceRoot resourceRoot, ResourceType type) throws IOException {
        final VirtualFile file = resourceRoot.getRoot();
        if (type == ResourceType.RESOURCE_ROOT) {
            VirtualFile persistenceXml = file.getChild(IN_META_INF);
            modifyPersistenceFile(unit, persistenceXml);
        }
    }

    protected void modifyPersistenceFile(DeploymentUnit unit, VirtualFile persistenceXml) throws IOException {
        if (persistenceXml != null && persistenceXml.exists()) {
            final InputStream stream = rewritePersistenceXml(persistenceXml);
            if (stream != null) {
                final File modifiedFile = tempDir.createFile(Long.toHexString(rng.nextLong()) + "_" + PERSISTENCE_XML, stream);
                final VirtualFile parent = persistenceXml.getParent();
                final ModifiedFileSystem mfs = ModifiedFileSystem.get(unit, parent.getPhysicalFile());
                mfs.addFile(PERSISTENCE_XML, modifiedFile);
                final Closeable closeable = VFS.mount(parent, mfs);
                unit.addToAttachmentList(ASSEMBLY_HANDLE, closeable);
            }
        }
    }

    protected InputStream rewritePersistenceXml(VirtualFile file) throws IOException {
        InputStream is = file.openStream();
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            VFSUtils.copyStream(is, baos);
            String content = baos.toString();
            if (content.contains(DATANUCLEUS_PROVIDER) == false && content.contains(DATANUCLEUS_GAE_PROVIDER) == false)
                return null; // we're not using DataNucleus

            final StringBuilder builder = new StringBuilder(content);
            int p = -1;
            while (true) {
                p = builder.indexOf(PROPERTIES, p + 1);
                if (p < 0) break;

                final int offset = p + PROPERTIES.length();
                builder.insert(offset, LOAD_AT_RUNTIME);
            }
            content = builder.toString();
            return new ByteArrayInputStream(content.getBytes());
        } catch (Exception e) {
            final IOException ioe = new IOException();
            ioe.initCause(e);
            throw ioe;
        } finally {
            VFSUtils.safeClose(is);
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        final AttachmentList<Closeable> closeables = context.getAttachment(ASSEMBLY_HANDLE);
        VFSUtils.safeClose(closeables);
        ModifiedFileSystem.remove(context);
    }
}
