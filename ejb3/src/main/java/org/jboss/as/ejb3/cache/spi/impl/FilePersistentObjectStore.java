/*
 * JBoss, Home of Professional Open Source
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.ejb3.cache.spi.impl;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import org.jboss.as.ejb3.EjbMessages;
import org.jboss.as.ejb3.cache.Cacheable;
import org.jboss.as.ejb3.cache.spi.PersistentObjectStore;
import org.jboss.logging.Logger;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.Unmarshaller;

/**
 * Stores objects in a directory via serialization.
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @author Paul Ferraro
 */
public class FilePersistentObjectStore<K extends Serializable, V extends Cacheable<K>> implements PersistentObjectStore<K, V> {
    private static final Logger log = Logger.getLogger(FilePersistentObjectStore.class);

    private final MarshallerFactory marshallerFactory = Marshalling.getMarshallerFactory("river", MarshallerFactory.class.getClassLoader());
    private final MarshallingConfiguration configuration;
    private final int subdirectoryCount;
    private final File baseDirectory;
    private File[] storageDirectories;

    private static class DeleteFileAction implements PrivilegedAction<Boolean> {
        File file;

        DeleteFileAction(File file) {
            this.file = file;
        }

        @Override
        public Boolean run() {
            return file.delete();
        }

        static boolean delete(File file) {
            DeleteFileAction action = new DeleteFileAction(file);
            return AccessController.doPrivileged(action);
        }
    }

    private static class FISAction implements PrivilegedExceptionAction<FileInputStream> {
        File file;

        FISAction(File file) {
            this.file = file;
        }

        @Override
        public FileInputStream run() throws FileNotFoundException {
            FileInputStream fis = new FileInputStream(file);
            return fis;
        }

        static FileInputStream open(File file) throws FileNotFoundException {
            FISAction action = new FISAction(file);
            FileInputStream fis = null;
            try {
                fis = AccessController.doPrivileged(action);
            } catch (PrivilegedActionException e) {
                throw (FileNotFoundException) e.getException();
            }
            return fis;
        }
    }

    private static class FOSAction implements PrivilegedExceptionAction<FileOutputStream> {
        File file;

        FOSAction(File file) {
            this.file = file;
        }

        @Override
        public FileOutputStream run() throws FileNotFoundException {
            FileOutputStream fis = new FileOutputStream(file);
            return fis;
        }

        static FileOutputStream open(File file) throws FileNotFoundException {
            FOSAction action = new FOSAction(file);
            FileOutputStream fos = null;
            try {
                fos = AccessController.doPrivileged(action);
            } catch (PrivilegedActionException e) {
                throw (FileNotFoundException) e.getException();
            }
            return fos;
        }
    }

    private static class MkdirsFileAction implements PrivilegedAction<Boolean> {
        File file;

        MkdirsFileAction(File file) {
            this.file = file;
        }

        @Override
        public Boolean run() {
            return file.mkdirs();
        }

        static boolean mkdirs(File file) {
            MkdirsFileAction action = new MkdirsFileAction(file);
            return AccessController.doPrivileged(action);
        }
    }

    public FilePersistentObjectStore(MarshallingConfiguration configuration, String directoryName, int subDirectoryCount) {
        this.configuration = configuration;
        this.baseDirectory = new File(directoryName);
        this.subdirectoryCount = subDirectoryCount;
    }

    protected File getFile(K key) {
        File base = null;
        if (storageDirectories != null) {
            int hash = (key.hashCode() & 0x7FFFFFFF) % storageDirectories.length;
            base = storageDirectories[hash];
        } else {
            base = baseDirectory;
        }
        return new File(base, String.valueOf(key) + ".ser");

    }

    @Override
    @SuppressWarnings("unchecked")
    public V load(K key) {
        File file = getFile(key);
        if (!file.exists())
            return null;

        log.tracef("Loading state from %s", file);
        try {
            Unmarshaller unmarshaller = this.marshallerFactory.createUnmarshaller(this.configuration);
            FileInputStream inputStream = null;
            try {
                inputStream = FISAction.open(file);
                unmarshaller.start(Marshalling.createByteInput(inputStream));
                try {
                    V value = (V) unmarshaller.readObject();
                    unmarshaller.finish();
                    return value;
                } finally {
                    unmarshaller.close();
                }
            } finally {
                safeClose(inputStream);
                DeleteFileAction.delete(file);
            }
        } catch (Exception e) {
            throw EjbMessages.MESSAGES.activationFailed(e, key);
        }
    }

    @Override
    public void start() {
        establishDirectory(baseDirectory);

        if (subdirectoryCount > 1) {
            storageDirectories = new File[subdirectoryCount];
            for (int i = 0; i < storageDirectories.length; i++) {
                File f = new File(baseDirectory, String.valueOf(i) + File.separatorChar);
                establishDirectory(f);
                storageDirectories[i] = f;
            }
        }
    }

    private void establishDirectory(File dir) {
        if (!dir.exists()) {
            if (!MkdirsFileAction.mkdirs(dir)) {
                throw EjbMessages.MESSAGES.passivationDirectoryCreationFailed(dir.getPath());
            }
            dir.deleteOnExit();
        }

        if (!dir.isDirectory()) {
            throw EjbMessages.MESSAGES.passivationPathNotADirectory(dir.getPath());
        }
    }

    @Override
    public void stop() {
        // TODO: implement
    }

    @Override
    public void store(V obj) {
        File file = getFile(obj.getId());
        file.deleteOnExit();
        log.tracef("Storing state to %s", file);
        try {
            Marshaller marshaller = this.marshallerFactory.createMarshaller(this.configuration);
            FileOutputStream outputStream = null;
            try {
                outputStream = FOSAction.open(file);
                marshaller.start(Marshalling.createByteOutput(outputStream));
                try {
                    marshaller.writeObject(obj);
                    marshaller.finish();
                } finally {
                    marshaller.close();
                }
            } finally {
                safeClose(outputStream);
            }
        } catch (IOException e) {
            throw EjbMessages.MESSAGES.passivationFailed(e, obj.getId());
        }
    }

    protected static void safeClose(final Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignore) {
                //
            }
        }
    }
}
