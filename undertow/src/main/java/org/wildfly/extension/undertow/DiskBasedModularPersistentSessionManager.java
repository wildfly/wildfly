/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.undertow;

import io.undertow.servlet.api.SessionPersistenceManager;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.marshalling.InputStreamByteInput;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.OutputStreamByteOutput;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.undertow.logging.UndertowLogger;
import org.xnio.IoUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Persistent session manager that stores persistent session information to disk
 *
 * @author Stuart Douglas
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class DiskBasedModularPersistentSessionManager extends AbstractPersistentSessionManager {
    private final String path;
    private final String pathRelativeTo;
    private final Supplier<PathManager> pathManager;
    private File baseDir;
    private PathManager.Callback.Handle callbackHandle;

    DiskBasedModularPersistentSessionManager(final Consumer<SessionPersistenceManager> serviceConsumer,
                                             final Supplier<ModuleLoader> moduleLoader,
                                             final Supplier<PathManager> pathManager,
                                             final String path, final String pathRelativeTo) {
        super(serviceConsumer, moduleLoader);
        this.pathManager = pathManager;
        this.path = path;
        this.pathRelativeTo = pathRelativeTo;
    }

    @Override
    public void stop(final StopContext stopContext) {
        super.stop(stopContext);
        if (callbackHandle != null) {
            callbackHandle.remove();
        }
    }

    @Override
    public void start(final StartContext startContext) throws StartException {
        super.start(startContext);
        if (pathRelativeTo != null) {
            callbackHandle = pathManager.get().registerCallback(pathRelativeTo, PathManager.ReloadServerCallback.create(), PathManager.Event.UPDATED, PathManager.Event.REMOVED);
        }
        baseDir = new File(pathManager.get().resolveRelativePathEntry(path, pathRelativeTo));
        if (!baseDir.exists()) {
            if (!baseDir.mkdirs()) {
                throw UndertowLogger.ROOT_LOGGER.failedToCreatePersistentSessionDir(baseDir);
            }
        }
        if (!baseDir.isDirectory()) {
            throw UndertowLogger.ROOT_LOGGER.invalidPersistentSessionDir(baseDir);
        }
    }


    @Override
    protected void persistSerializedSessions(String deploymentName, Map<String, SessionEntry> serializedData) throws IOException {
        File file = new File(baseDir, deploymentName);
        FileOutputStream out = new FileOutputStream(file, false);
        try {
            Marshaller marshaller = createMarshaller();
            try {
                marshaller.start(new OutputStreamByteOutput(out));
                marshaller.writeObject(serializedData);
                marshaller.finish();
            } finally {
                marshaller.close();
            }
        } finally {
            IoUtils.safeClose(out);
        }
    }

    @Override
    protected Map<String, SessionEntry> loadSerializedSessions(String deploymentName) throws IOException {
        File file = new File(baseDir, deploymentName);
        if (!file.exists()) {
            return null;
        }
        FileInputStream in = new FileInputStream(file);
        try {
            Unmarshaller unMarshaller = createUnmarshaller();
            try {
                try {
                    unMarshaller.start(new InputStreamByteInput(in));
                    return (Map<String, SessionEntry>) unMarshaller.readObject();
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                } finally {
                    unMarshaller.finish();
                }
            } finally {
                unMarshaller.close();
            }
        } finally {
            IoUtils.safeClose(in);
        }

    }
}
