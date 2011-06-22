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

package org.jboss.as.controller.persistence;

import javax.xml.stream.XMLStreamWriter;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 * {@link ConfigurationPersister.PersistenceResource} that persists to a file upon commit.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class FilePersistenceResource implements ConfigurationPersister.PersistenceResource {

    protected static final Logger log = Logger.getLogger("org.jboss.as.controller");

    private ExposedByteArrayOutputStream marshalled;
    private final File fileName;
    private final AbstractConfigurationPersister persister;

    FilePersistenceResource(final ModelNode model, final File fileName, final AbstractConfigurationPersister persister) throws ConfigurationPersistenceException {
        this.fileName = fileName;
        this.persister = persister;
        marshalled = new ExposedByteArrayOutputStream(1024 * 8);
        try {
            try {
                BufferedOutputStream output = new BufferedOutputStream(marshalled);
                persister.marshallAsXml(model, output);
                output.close();
                marshalled.close();
            } finally {
                safeClose(marshalled);
            }
        } catch (Exception e) {
            throw new ConfigurationPersistenceException("Failed to marshal configuration", e);
        }
    }

    @Override
    public void commit() {
        if (marshalled == null) {
            throw new IllegalStateException("rollback() has already been invoked");
        }
        try {
            final FileOutputStream fos = new FileOutputStream(fileName);
            final InputStream is = marshalled.getInputStream();
            try {
                BufferedOutputStream output = new BufferedOutputStream(fos);
                byte[] bytes = new byte[1024];
                int read;
                while ((read = is.read(bytes)) > -1) {
                    output.write(bytes, 0, read);
                }
                output.close();
                fos.close();
                is.close();
            } finally {
                safeClose(fos);
                safeClose(is);
            }
        } catch (Exception e) {
            log.errorf(e, "Failed to store configuration to %s", fileName.getName());
        }
    }

    @Override
    public void rollback() {
        marshalled = null;
    }

    private static void safeClose(final Closeable closeable) {
        if (closeable != null) try {
            closeable.close();
        } catch (Throwable t) {
            log.errorf(t, "Failed to close resource %s", closeable);
        }
    }
}
