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

import static org.jboss.as.controller.ControllerLogger.MGMT_OP_LOGGER;

import java.io.File;

import org.jboss.dmr.ModelNode;

/**
 * {@link ConfigurationPersister.PersistenceResource} that persists to a file upon commit.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class FilePersistenceResource extends AbstractFilePersistenceResource {

    protected final File fileName;

    FilePersistenceResource(final ModelNode model, final File fileName, final AbstractConfigurationPersister persister) throws ConfigurationPersistenceException {
        super(model, persister);
        this.fileName = fileName;

    }


    @Override
    protected void doCommit(ExposedByteArrayOutputStream marshalled) {
        final File tempFileName = FilePersistenceUtils.createTempFile(fileName);
        try {
            FilePersistenceUtils.writeToTempFile(marshalled, tempFileName);
            FilePersistenceUtils.moveTempFileToMain(tempFileName, fileName);
        } catch (Exception e) {
            MGMT_OP_LOGGER.failedToStoreConfiguration(e, fileName.getName());
        } finally {
            if (tempFileName.exists() && !tempFileName.delete()) {
                MGMT_OP_LOGGER.cannotDeleteTempFile(tempFileName.getName());
                tempFileName.deleteOnExit();
            }
        }
    }
}
