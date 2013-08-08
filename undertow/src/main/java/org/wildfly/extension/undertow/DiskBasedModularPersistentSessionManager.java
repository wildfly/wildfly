package org.wildfly.extension.undertow;

import org.jboss.as.controller.services.path.PathManager;
import org.jboss.marshalling.InputStreamByteInput;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.OutputStreamByteOutput;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.xnio.IoUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Persistent session manager that stores persistent session information to disk
 *
 * @author Stuart Douglas
 */
public class DiskBasedModularPersistentSessionManager extends AbstractPersistentSessionManager {
    private final String path;
    private final String pathRelativeTo;
    private File baseDir;
    private PathManager.Callback.Handle callbackHandle;

    private final InjectedValue<PathManager> pathManager = new InjectedValue<PathManager>();

    public DiskBasedModularPersistentSessionManager(String path, String pathRelativeTo) {
        this.path = path;
        this.pathRelativeTo = pathRelativeTo;
    }

    @Override
    public synchronized void stop(StopContext stopContext) {
        super.stop(stopContext);
        if (callbackHandle != null) {
            callbackHandle.remove();
        }
    }

    @Override
    public synchronized void start(StartContext startContext) throws StartException {
        super.start(startContext);
        if (pathRelativeTo != null) {
            callbackHandle = pathManager.getValue().registerCallback(pathRelativeTo, PathManager.ReloadServerCallback.create(), PathManager.Event.UPDATED, PathManager.Event.REMOVED);
        }
        baseDir = new File(pathManager.getValue().resolveRelativePathEntry(path, pathRelativeTo));
        if (!baseDir.exists()) {
            if (!baseDir.mkdirs()) {
                throw UndertowMessages.MESSAGES.failedToCreatePersistentSessionDir(baseDir);
            }
        }
        if (!baseDir.isDirectory()) {
            throw UndertowMessages.MESSAGES.invalidPersistentSessionDir(baseDir);
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

    public InjectedValue<PathManager> getPathManager() {
        return pathManager;
    }
}
