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
import org.wildfly.extension.undertow.logging.UndertowLogger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
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
                            UndertowLogger.ROOT_LOGGER.failedToPersistSessionAttribute(sessionAttribute.getKey(), sessionAttribute.getValue(), sessionEntry.getKey(), e);
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

    protected static final class SessionEntry implements Serializable {
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
