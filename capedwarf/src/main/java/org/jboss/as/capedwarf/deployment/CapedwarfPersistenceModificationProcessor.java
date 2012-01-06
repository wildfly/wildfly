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
import org.jboss.as.server.deployment.module.TempFileProviderService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.vfs.TempDir;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileAssembly;

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

    static final String PROVIDER_START = "<provider>";
    static final String PROVIDER_END = "</provider>";
    static final String PROVIDER_REGEXP = PROVIDER_START + "[.]*" + PROVIDER_END;
    static final String HIBERNATE_PROVIDER = PROVIDER_START + Configuration.PROVIDER_CLASS_HIBERNATE + PROVIDER_END;
    static final String HIBERNATE_OGM_PROVIDER = PROVIDER_START + Configuration.PROVIDER_CLASS_HIBERNATE_OGM + PROVIDER_END;
    static final String NON_JTA_DS = "<non-jta-data-source>";
    static final String NON_JTA_DS_DEFINITION = NON_JTA_DS + "java:jboss/datasources/ExampleDS</non-jta-data-source>";
    static final String PROPERTIES = "<properpties>";
    static final String DIALECT_PROPERTY = "<property name=\"hibernate.dialect\" value=\"" + DEFAULT_DIALECT + "\"/>";
    static final String CREATE_DROP = "<property name=\"hibernate.hbm2ddl.auto\" value=\"create-drop\"/>";

    static final AttachmentKey<AttachmentList<Closeable>> ASSEMBLY_HANDLE = AttachmentKey.createList(Closeable.class);
    static final Random rng = new Random();

    final TempDir tempDir;

    public CapedwarfPersistenceModificationProcessor(ServiceTarget serviceTarget) {
        try {
            tempDir = TempFileProviderService.provider().createTempDir(CAPEDWARF);
            final ServiceBuilder<TempDir> builder = serviceTarget.addService(ServiceName.JBOSS.append(CAPEDWARF).append("tempDir"), new Service<TempDir>() {
                public void start(StartContext context) throws StartException {
                }

                public void stop(StopContext context) {
                    VFSUtils.safeClose(tempDir);
                }

                public TempDir getValue() throws IllegalStateException, IllegalArgumentException {
                    return tempDir;
                }
            });
            builder.setInitialMode(ServiceController.Mode.ACTIVE).install();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    protected void modifyPersistenceInfo(DeploymentUnit unit, ResourceRoot resourceRoot) throws IOException {
        final VirtualFile file = resourceRoot.getRoot();

        VirtualFile persistenceXml = file.getChild(IN_WEB_INF);
        modifyPersistenceFile(unit, persistenceXml);

        persistenceXml = file.getChild(IN_META_INF);
        modifyPersistenceFile(unit, persistenceXml);
    }

    protected void modifyPersistenceFile(DeploymentUnit unit, VirtualFile persistenceXml) throws IOException {
        if (persistenceXml != null && persistenceXml.exists()) {
            final VirtualFileAssembly assembly = new VirtualFileAssembly();
            final File modifiedFile = tempDir.createFile(Long.toHexString(rng.nextLong()) + "_" + PERSISTENCE_XML, rewritePersistenceXml(persistenceXml));
            assembly.add(PERSISTENCE_XML, modifiedFile);
            final Closeable closeable = VFS.mountAssembly(assembly, persistenceXml);
            unit.addToAttachmentList(ASSEMBLY_HANDLE, closeable);
        }
    }

    protected InputStream rewritePersistenceXml(VirtualFile file) throws IOException {
        InputStream is = file.openStream();
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            VFSUtils.copyStream(is, baos);
            String content = baos.toString();
            content = content.replaceAll(PROVIDER_REGEXP, HIBERNATE_PROVIDER);
            final StringBuilder builder = new StringBuilder(content);
            final int ds = builder.indexOf(NON_JTA_DS);
            if (ds < 0) {
                while (true) {
                    int pe = builder.indexOf(PROVIDER_END); // TODO -- assume provider is always defined
                    if (pe < 0) break;
                    builder.insert(pe + PROVIDER_END.length(), NON_JTA_DS_DEFINITION);
                }
            }
            final int p = builder.indexOf(PROPERTIES);
            if (p > 0) {
                builder.insert(p + PROPERTIES.length(), CREATE_DROP);
                builder.insert(p + PROPERTIES.length(), DIALECT_PROPERTY);
            } else {
                // TODO
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
    }
}
