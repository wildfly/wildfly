package org.wildfly.extension.undertow;

import io.undertow.servlet.UndertowServletLogger;
import io.undertow.servlet.api.SessionPersistenceManager;
import org.jboss.marshalling.ByteBufferInput;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassResolver;
import org.jboss.marshalling.OutputStreamByteOutput;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.marshalling.river.RiverMarshallerFactory;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Persistent session manager
 *
 * @author Stuart Douglas
 */
public abstract class AbstractPersistentSessionManager implements SessionPersistenceManager, Service<SessionPersistenceManager> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("undertow", "persistent-session-manager");

    private MarshallerFactory factory;
    private MarshallingConfiguration configuration;

    private final InjectedValue<ModuleLoader> moduleLoaderInjectedValue = new InjectedValue<>();

    @Override
    public void persistSessions(String deploymentName, Map<String, PersistentSession> sessionData) {
        try {
            final Marshaller marshaller = createMarshaller();
            try {
                final Map<String, SessionEntry> serializedData = new HashMap<String, SessionEntry>();
                for (Map.Entry<String, PersistentSession> sessionEntry : sessionData.entrySet()) {
                    Map<String, byte[]> data = new HashMap<String, byte[]>();
                    for (Map.Entry<String, Object> sessionAttribute : sessionEntry.getValue().getSessionData().entrySet()) {
                        try {
                            final ByteArrayOutputStream out = new ByteArrayOutputStream();
                            marshaller.start(new OutputStreamByteOutput(out));
                            marshaller.writeObject(sessionAttribute.getValue());
                            marshaller.finish();
                            data.put(sessionAttribute.getKey(), out.toByteArray());
                        } catch (Exception e) {
                            UndertowServletLogger.ROOT_LOGGER.failedToPersistSessionAttribute(sessionAttribute.getKey(), sessionAttribute.getValue(), sessionEntry.getKey());
                        }
                    }
                    serializedData.put(sessionEntry.getKey(), new SessionEntry(sessionEntry.getValue().getExpiration(), data));
                }
                persistSerializedSessions(deploymentName, serializedData);
            } finally {
                marshaller.close();
            }
        } catch (Exception e) {
            UndertowServletLogger.ROOT_LOGGER.failedToPersistSessions(e);
        }

    }

    protected abstract void persistSerializedSessions(String deploymentName, Map<String, SessionEntry> serializedData) throws IOException;

    protected abstract Map<String, SessionEntry> loadSerializedSessions(final String deploymentName) throws IOException;

    @Override
    public Map<String, PersistentSession> loadSessionAttributes(String deploymentName, final ClassLoader classLoader) {
        try {
            Unmarshaller unmarshaller = createUnmarshaller();
            try {
                long time = System.currentTimeMillis();
                Map<String, SessionEntry> data = loadSerializedSessions(deploymentName);
                if (data != null) {
                    Map<String, PersistentSession> ret = new HashMap<String, PersistentSession>();
                    for (Map.Entry<String, SessionEntry> sessionEntry : data.entrySet()) {
                        if (sessionEntry.getValue().expiry.getTime() > time) {
                            Map<String, Object> session = new HashMap<String, Object>();
                            for (Map.Entry<String, byte[]> sessionAttribute : sessionEntry.getValue().data.entrySet()) {
                                unmarshaller.start(new ByteBufferInput(ByteBuffer.wrap(sessionAttribute.getValue())));
                                session.put(sessionAttribute.getKey(), unmarshaller.readObject());
                                unmarshaller.finish();
                            }
                            ret.put(sessionEntry.getKey(), new PersistentSession(sessionEntry.getValue().expiry, session));
                        }
                    }
                    return ret;
                }
            } finally {
                unmarshaller.close();
            }
        } catch (Exception e) {
            UndertowServletLogger.ROOT_LOGGER.failedtoLoadPersistentSessions(e);
        }
        return null;
    }

    protected Marshaller createMarshaller() throws IOException {
        return factory.createMarshaller(configuration);
    }

    protected Unmarshaller createUnmarshaller() throws IOException {
        return factory.createUnmarshaller(configuration);
    }

    @Override
    public void clear(String deploymentName) {
    }

    @Override
    public synchronized void start(StartContext startContext) throws StartException {
        final RiverMarshallerFactory factory = new RiverMarshallerFactory();
        final MarshallingConfiguration configuration = new MarshallingConfiguration();
        configuration.setClassResolver(ModularClassResolver.getInstance(moduleLoaderInjectedValue.getValue()));
        this.configuration = configuration;
        this.factory = factory;
    }

    @Override
    public synchronized void stop(StopContext stopContext) {
    }

    @Override
    public synchronized SessionPersistenceManager getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public InjectedValue<ModuleLoader> getModuleLoaderInjectedValue() {
        return moduleLoaderInjectedValue;
    }

    protected static final class SessionEntry {
        private final Date expiry;
        private final Map<String, byte[]> data;

        private SessionEntry(Date expiry, Map<String, byte[]> data) {
            this.expiry = expiry;
            this.data = data;
        }

        public Date getExpiry() {
            return expiry;
        }

        public Map<String, byte[]> getData() {
            return data;
        }
    }
}
