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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import javax.xml.namespace.QName;

import org.jboss.as.protocol.StreamUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;

/**
 * An XML configuration persister which backs up the old file before overwriting it.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class BackupXmlConfigurationPersister extends XmlConfigurationPersister {

    /**
     * Construct a new instance.
     *
     * @param fileName the configuration base file name
     * @param rootElement the root element of the configuration file
     * @param rootParser the root model parser
     * @param rootDeparser the root model deparser
     */
    public BackupXmlConfigurationPersister(final File fileName, final QName rootElement, final XMLElementReader<List<ModelNode>> rootParser, final XMLElementWriter<ModelMarshallingContext> rootDeparser) {
        super(fileName, rootElement, rootParser, rootDeparser);
    }

    /** {@inheritDoc} */
    @Override
    protected void backup(final File fileName) throws ConfigurationPersistenceException {
        File backup = new File(fileName.getParent(), fileName.getName() + ".last-known-good");
        try {
            moveFile(fileName, backup);
        } catch (IOException e) {
            throw new ConfigurationPersistenceException("Failed to back up " + fileName, e);
        }
    }

    private void moveFile(File file, File backup) throws IOException {

        if (backup.exists())
            backup.delete();

        if (!file.renameTo(backup) && file.exists()) {
            final FileInputStream fis = new FileInputStream(file);
            try {
                final FileOutputStream fos = new FileOutputStream(backup);
                try {
                    StreamUtils.copyStream(fis, fos);
                    fos.close();
                } finally {
                    StreamUtils.safeClose(fos);
                }
            } finally {
                StreamUtils.safeClose(fis);
            }
        }
    }
}
