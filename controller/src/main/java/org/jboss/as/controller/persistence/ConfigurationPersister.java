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

import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

/**
 * The configuration persister for a model.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ConfigurationPersister {

    /**
     * Callback for use by callers to {@link ConfigurationPersister#store(org.jboss.dmr.ModelNode, java.util.Set)}
     * to control whether the stored model should be flushed to permanent storage.
     */
    interface PersistenceResource {

        /**
         * Flush the stored model to permanent storage.
         */
        void commit();

        /**
         * Discard the changes.
         */
        void rollback();
    }

    /**
     * Persist the given configuration model.
     *
     * @param model the model to persist
     * @param affectedAddresses
     *
     * @return callback to use to control whether the stored model should be flushed to persistent storage
     */
    PersistenceResource store(ModelNode model, Set<PathAddress> affectedAddresses) throws ConfigurationPersistenceException;

    /**
     * Marshals the given configuration model to XML, writing to the given stream.
     *
     * @param model  the model to persist
     * @param output the stream
     * @throws ConfigurationPersistenceException
     */
    void marshallAsXml(final ModelNode model, final OutputStream output) throws ConfigurationPersistenceException;

    /**
     * Load the configuration model, returning it as a list of updates to be
     * executed by the controller.
     */
    List<ModelNode> load() throws ConfigurationPersistenceException;

    /**
     * Called once the xml has been successfully parsed, translated into updates, executed in the target controller
     * and all services have started successfully
     */
    void successfulBoot() throws ConfigurationPersistenceException;

    /**
     * Take a snapshot of the current configuration
     *
     * @return the location of the snapshot
     * @return the file location of the snapshot
     * @throws ConfigurationPersistenceException if a problem happened when creating the snapshot
     */
    String snapshot() throws ConfigurationPersistenceException;

    /**
     * Gets the names of the snapshots in the snapshots directory
     *
     * @return the snapshot info. This will never return null
     */
    SnapshotInfo listSnapshots();

    /**
     * Deletes a snapshot using its name.
     *
     * @param name the name of the snapshot (as returned by {@link SnapshotInfo#names()} returned from {@link #listSnapshots()}. The whole name is not
     * needed, just enough to uniquely identify it.
     * @throws IllegalArgumentException if there is no snapshot with the given name, or if the name resolves to more than one snapshot.
     */
    void deleteSnapshot(String name);

    /**
     * Contains the info about the configuration snapshots
     */
    public interface SnapshotInfo {
        /**
         * Gets the snapshots directory
         *
         * @return the snapshots directory
         */
        String getSnapshotDirectory();

        /**
         * Gets the names of the snapshot files in the snapshots directory
         *
         * @return the snapshot names. If there are none, an empty list is returned
         */
        List<String> names();
    }

    SnapshotInfo NULL_SNAPSHOT_INFO = new SnapshotInfo() {

        @Override
        public List<String> names() {
            return Collections.emptyList();
        }

        @Override
        public String getSnapshotDirectory() {
            return "";
        }
    };

}
