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

package org.jboss.as.jacorb.service;

import java.net.InetSocketAddress;
import java.util.Properties;

import org.jboss.as.jacorb.JacORBLogger;
import org.jboss.as.jacorb.JacORBSubsystemConstants;
import org.jboss.as.jacorb.naming.jndi.CorbaUtils;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.omg.CORBA.ORB;

/**
 * <p>
 * This class implements a {@code Service} that creates and installs a CORBA {@code ORB}.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class CorbaORBService implements Service<ORB> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("jacorb", "orb-service");

    private static final Properties properties = new Properties();

    private final InjectedValue<SocketBinding> jacORBSocketBindingInjector = new InjectedValue<SocketBinding>();

    private final InjectedValue<SocketBinding> jacORBSSLSocketBindingInjector = new InjectedValue<SocketBinding>();

    private volatile ORB orb;

    /**
     * <p>
     * Creates an instance of {@code CorbaORBService} with the specified {@code ORBImplementation} and initializers.
     * </p>
     *
     * @param props a {@code Properties} instance containing the JacORB subsystem configuration properties.
     */
    public CorbaORBService(Properties props) {
        if (props != null) {
            properties.putAll(props);
        }
    }

    @Override
    public void start(StartContext context) throws StartException {
        JacORBLogger.ROOT_LOGGER.debugServiceStartup(context.getController().getName().getCanonicalName());

        try {
            // set the ORBClass and ORBSingleton class as system properties.
            properties.setProperty(JacORBSubsystemConstants.ORB_CLASS, JacORBSubsystemConstants.JACORB_ORB_CLASS);
            properties.setProperty(JacORBSubsystemConstants.ORB_SINGLETON_CLASS, JacORBSubsystemConstants.JacORB_ORB_SINGLETON_CLASS);
            SecurityActions.setSystemProperty(JacORBSubsystemConstants.ORB_CLASS, JacORBSubsystemConstants.JACORB_ORB_CLASS);
            SecurityActions.setSystemProperty(JacORBSubsystemConstants.ORB_SINGLETON_CLASS, JacORBSubsystemConstants.JacORB_ORB_SINGLETON_CLASS);

            // set the JacORB IIOP and IIOP/SSL ports from the respective socket bindings.
            if (this.jacORBSocketBindingInjector.getValue() != null) {
                InetSocketAddress address = this.jacORBSocketBindingInjector.getValue().getSocketAddress();
                properties.setProperty(JacORBSubsystemConstants.ORB_ADDRESS, address.getHostName());
                properties.setProperty(JacORBSubsystemConstants.ORB_PORT, String.valueOf(address.getPort()));
            }
            if (this.jacORBSSLSocketBindingInjector.getValue() != null) {
                InetSocketAddress address = this.jacORBSSLSocketBindingInjector.getValue().getSocketAddress();
                properties.setProperty(JacORBSubsystemConstants.ORB_SSL_PORT, String.valueOf(address.getPort()));
                if (!properties.containsKey(JacORBSubsystemConstants.ORB_ADDRESS)) {
                    properties.setProperty(JacORBSubsystemConstants.ORB_ADDRESS, address.getHostName());
                }
            }

            // configure the naming service initial reference.
            String rootContext = properties.getProperty(JacORBSubsystemConstants.NAMING_ROOT_CONTEXT);
            String host = properties.getProperty(JacORBSubsystemConstants.ORB_ADDRESS);
            String port = properties.getProperty(JacORBSubsystemConstants.ORB_PORT);
            properties.setProperty(JacORBSubsystemConstants.JACORB_NAME_SERVICE_INIT_REF,
                    "corbaloc::" + host + ":" + port + "/" + rootContext);

            // export the naming service corbaloc if necessary.
            String exportCorbalocProperty = properties.getProperty(JacORBSubsystemConstants.NAMING_EXPORT_CORBALOC, "on");
            if (exportCorbalocProperty.equalsIgnoreCase("on")) {
                properties.setProperty(JacORBSubsystemConstants.JACORB_NAME_SERVICE_MAP_KEY, rootContext);
            }

            // initialize the ORB - the thread context classloader needs to be adjusted as the ORB classes are loaded via reflection.
            ClassLoader loader = SecurityActions.getThreadContextClassLoader();
            try {
                SecurityActions.setThreadContextClassLoader(SecurityActions.getClassLoader(this.getClass()));
                this.orb = ORB.init(new String[0], properties);
                // initialize the ORBSingleton.
                ORB.init();
            } finally {
                // restore the thread context classloader.
                SecurityActions.setThreadContextClassLoader(loader);
            }

            // start the ORB in a separate thread.
            Thread orbThread = SecurityActions.createThread(new ORBRunner(this.orb), "ORB Run Thread");
            orbThread.start();

            // bind the ORB to JNDI under java:/jboss/ORB.
            ServiceTarget target = context.getChildTarget();
            CorbaServiceUtil.bindObject(target, "ORB", this.orb);
        } catch (Exception e) {
            throw new StartException(e);
        }

        CorbaUtils.setOrbProperties(properties);

        JacORBLogger.ROOT_LOGGER.corbaORBServiceStarted();
    }

    @Override
    public void stop(StopContext context) {
        JacORBLogger.ROOT_LOGGER.debugServiceStop(context.getController().getName().getCanonicalName());
        // stop the ORB asynchronously.
        context.asynchronous();
        Thread destroyThread = SecurityActions.createThread(new ORBDestroyer(this.orb, context), "ORB Destroy Thread");
        destroyThread.start();
    }

    @Override
    public ORB getValue() throws IllegalStateException, IllegalArgumentException {
        return this.orb;
    }

    /**
     * <p>
     * Obtains a reference to the JacORB IIOP socket binding injector. This injector is used to inject a {@code ServiceBinding}
     * containing the IIOP socket properties.
     * </p>
     *
     * @return a reference to the {@code Injector<SocketBinding>} used to inject the JacORB IIOP socket properties.
     */
    public Injector<SocketBinding> getJacORBSocketBindingInjector() {
        return this.jacORBSocketBindingInjector;
    }

    /**
     * <p>
     * Obtains a reference to the JacORB IIOP/SSL socket binding injector. This injector is used to inject a
     * {@code ServiceBinding} containing the IIOP/SSL socket properties.
     * </p>
     *
     * @return a reference to the {@code Injector<SocketBinding>} used to inject the JacORB IIOP/SSL socket properties.
     */
    public Injector<SocketBinding> getJacORBSSLSocketBindingInjector() {
        return this.jacORBSSLSocketBindingInjector;
    }

    /**
     * <p>
     * Gets the value of the specified ORB property. All ORB properties can be queried using this method. This includes
     * the properties that have been explicitly set by this service prior to creating the ORB and all JacORB properties
     * that have been specified in the JacORB subsystem configuration.
     * </p>
     *
     * @param key the property key.
     * @return the property value or {@code null} if the property with the specified key hasn't been configured.
     */
    public static String getORBProperty(String key) {
        return properties.getProperty(key);
    }

    public static ORB getCurrent() {
        return (ORB) CurrentServiceContainer.getServiceContainer().getRequiredService(SERVICE_NAME).getValue();
    }

    /**
     * <p>
     * The {@code ORBRunner} calls the blocking {@code run()} method on the specified {@code ORB} instance and is used
     * to start the {@code ORB} in a dedicated thread.
     * </p>
     */
    private class ORBRunner implements Runnable {

        private ORB orb;

        public ORBRunner(ORB orb) {
            this.orb = orb;
        }

        @Override
        public void run() {
            this.orb.run();
        }
    }

    /**
     * <p>
     * The {@code ORBDestroyer} is responsible for destroying the specified {@code ORB} instance without blocking the
     * thread that called {@code stop} on {@code CorbaORBService}.
     * </p>
     */
    private class ORBDestroyer implements Runnable {

        private ORB orb;

        private StopContext context;

        public ORBDestroyer(ORB orb, StopContext context) {
            this.orb = orb;
            this.context = context;
        }

        @Override
        public void run() {
            // orb.destroy blocks until the ORB has shutdown. We must signal the context when the process is complete.
            try {
                CorbaUtils.setOrbProperties(null);
                this.orb.destroy();
            } finally {
                this.context.complete();
            }
        }
    }
}
