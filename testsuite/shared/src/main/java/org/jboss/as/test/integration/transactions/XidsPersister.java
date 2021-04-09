/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2020, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.transactions;

import org.jboss.logging.Logger;

import javax.transaction.xa.Xid;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

/**
 * An utility class which is capable to persist {@link TestXAResource} {@link Xid} records
 * to the file under <code>jboss.server.data.dir</code>.
 * This capability is needed when server crash with recovery is tested.
 */
public class XidsPersister {
    private static final Logger log = Logger.getLogger(XidsPersister.class);

    private final String fileToPersit;
    private final File directoryToPersist;

    XidsPersister(String fileToPersit) {
        this.fileToPersit = fileToPersit;
        this.directoryToPersist = new File(System.getProperty("jboss.server.data.dir"));
    }

    public XidsPersister(File directoryToPersist, String fileToPersit) {
        if (fileToPersit == null || directoryToPersist == null) {
            throw new NullPointerException("directoryToPersist: " + directoryToPersist + ", fileToPersist: " + fileToPersit);
        }
        this.fileToPersit = fileToPersit;
        this.directoryToPersist = directoryToPersist;
    }

    /**
     * Writing the Xids as they are (expecting being {@link java.io.Serializable})
     * to the persisted log file. It replaces the content existing before this method is executed.
     *
     * When {@code null} is provided as parameter then it's equal to removing the content
     * of the file and replacing it with empty set of Xids.
     *
     * @param xidsToSave {@link Xid}s to be saved to file
     */
    public synchronized void writeToDisk(Collection<Xid> xidsToSave) {
        Path logFile = getLogFile();
        FileOutputStream fos = null;
        if (xidsToSave == null) { // when null is provided then save just a clean set of Xids
            xidsToSave = new ArrayList<>();
        }
        try {
            fos = new FileOutputStream(logFile.toFile());
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(xidsToSave);
            log.debugf("Xids %s were written as the new state of the file %s", xidsToSave, logFile);
        } catch (IOException ioe) {
            log.errorf(ioe, "Cannot write xids %s to persistent file %s", xidsToSave, logFile);
        } finally {
             try {
                 if (fos != null) fos.close();
             } catch (IOException ioe) {
                 log.debugf(ioe,"Cannot close FileOutputStream for file %s", logFile);
             }
        }
    }

    /**
     * Returning the list of {@link Xid} which were saved to the persisted log file.
     *
     * @return list of {@link Xid}s
     */
    @SuppressWarnings("unchecked")
    public synchronized Collection<Xid> recoverFromDisk() {
        Path logFile = getLogFile();
        if (!logFile.toFile().exists()) {
            log.debugf("There is no file %s with recovery data for the test XAResource, no data for recovery", logFile);
            return new ArrayList<>();
        }

        log.debugf("There is found file %s for transaction recovery of the test XAResource", logFile);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(logFile.toFile());
            ObjectInputStream ois = new ObjectInputStream(fis);
            Collection<Xid> xids = (Collection<Xid>) ois.readObject();
            log.infof("Number of xids for recovery is %d.%nContent: %s", xids.size(), xids);
            return xids;
        } catch (Exception e) {
            log.errorf(e, "Cannot load recovery data for test XAResource from file %s", logFile);
            return new ArrayList<>();
        } finally {
            try {
                if (fis != null) fis.close();
            } catch (IOException ioe) {
                log.debugf(ioe,"Cannot close FileInputStrem for file %s", logFile);
            }
        }
    }

    Path getLogFile() {
        try {
            directoryToPersist.mkdirs();
            return directoryToPersist.toPath().resolve(this.fileToPersit);
        } catch (InvalidPathException e) {
            throw new IllegalStateException("Cannot resolve path of recovery file " + this.fileToPersit
                    + " for storing test XAResource data persistently");
        }
    }
}