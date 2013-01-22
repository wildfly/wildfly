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

package org.jboss.as.jdkorb.service;

import java.net.InetSocketAddress;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

import com.sun.corba.se.impl.orb.ORBImpl;
import com.sun.corba.se.impl.orb.ORBSingleton;
import com.sun.corba.se.impl.orbutil.ORBConstants;
import org.jboss.as.iiop.IIOPServiceNames;
import org.jboss.as.iiop.OrbService;
import org.jboss.as.iiop.naming.jndi.CorbaUtils;
import org.jboss.as.iiop.service.CorbaServiceUtil;
import org.jboss.as.jdkorb.JDKORBLogger;
import org.jboss.as.jdkorb.JDKORBSubsystemConstants;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Policy;
import org.omg.CORBA.PolicyError;

/**
 * <p>
 * This class implements a {@code Service} that creates and installs a CORBA {@code ORB}.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class JDKCorbaORBService implements Service<ORB>, OrbService {


    private final InjectedValue<ExecutorService> executorInjector = new InjectedValue<ExecutorService>();

    private final InjectedValue<SocketBinding> sunORBSocketBindingInjector = new InjectedValue<SocketBinding>();

    private final InjectedValue<SocketBinding> sunORBSSLSocketBindingInjector = new InjectedValue<SocketBinding>();

    private volatile ORB orb;
    private volatile Properties properties;
    private final Properties initialProperties;

    /**
     * <p>
     * Creates an instance of {@code CorbaORBService} with the specified {@code ORBImplementation} and initializers.
     * </p>
     *
     * @param props a {@code Properties} instance containing the SunORB subsystem configuration properties.
     */
    public JDKCorbaORBService(Properties props) {
        initialProperties = new Properties();
        if (props != null) {
            initialProperties.putAll(props);
        }
    }

    @Override
    public void start(StartContext context) throws StartException {
        final Properties properties = new Properties();
        properties.putAll(initialProperties);
        JDKORBLogger.ROOT_LOGGER.debugServiceStartup(context.getController().getName().getCanonicalName());

        try {
            // set the ORBClass and ORBSingleton class as system properties.
            properties.setProperty(JDKORBSubsystemConstants.ORB_CLASS, ORBImpl.class.getName());
            properties.setProperty(JDKORBSubsystemConstants.ORB_SINGLETON_CLASS, ORBSingleton.class.getName());
            SecurityActions.setSystemProperty(JDKORBSubsystemConstants.ORB_CLASS, ORBImpl.class.getName());
            SecurityActions.setSystemProperty(JDKORBSubsystemConstants.ORB_SINGLETON_CLASS, ORBSingleton.class.getName());

            // set the SunORB IIOP and IIOP/SSL ports from the respective socket bindings.
            if (this.sunORBSocketBindingInjector.getValue() != null) {
                InetSocketAddress address = this.sunORBSocketBindingInjector.getValue().getSocketAddress();
                properties.setProperty(ORBConstants.SERVER_HOST_PROPERTY, address.getAddress().getHostAddress());
                properties.setProperty(ORBConstants.SERVER_PORT_PROPERTY, String.valueOf(address.getPort()));
                properties.setProperty(ORBConstants.INITIAL_HOST_PROPERTY, address.getAddress().getHostAddress());
                properties.setProperty(ORBConstants.INITIAL_PORT_PROPERTY, String.valueOf(address.getPort()));
                properties.setProperty(ORBConstants.PERSISTENT_NAME_SERVICE_NAME, address.getAddress().getHostAddress());
                properties.setProperty(ORBConstants.PERSISTENT_SERVER_PORT_PROPERTY, String.valueOf(address.getPort()));
                properties.setProperty(ORBConstants.ORB_SERVER_ID_PROPERTY, "1"); //fixme
            }
            if (this.sunORBSSLSocketBindingInjector.getValue() != null) {
                InetSocketAddress address = this.sunORBSSLSocketBindingInjector.getValue().getSocketAddress();
                //TODO: not sure what the Sun equivlent is
                properties.setProperty(JDKORBSubsystemConstants.ORB_SSL_PORT, String.valueOf(address.getPort()));
                if (!properties.containsKey(ORBConstants.SERVER_HOST_PROPERTY)) {
                    properties.setProperty(ORBConstants.SERVER_HOST_PROPERTY, address.getAddress().getHostAddress());
                    properties.setProperty(ORBConstants.INITIAL_HOST_PROPERTY, address.getAddress().getHostAddress());
                }
            }

            this.properties = properties;

            //configure the naming service initial reference.
            //String rootContext = properties.getProperty(SunORBSubsystemConstants.NAMING_ROOT_CONTEXT);
            //String host = properties.getProperty(SunORBSubsystemConstants.ORB_ADDRESS);
            //String port = properties.getProperty(SunORBSubsystemConstants.ORB_PORT);

            //properties.setProperty(ORBConstants.NAME_SERVICE_SERVER_ID,
            //        "corbaloc::" + NetworkUtils.formatPossibleIpv6Address(host) + ":" + port + "/" + rootContext);

            // export the naming service corbaloc if necessary.
            //String exportCorbalocProperty = properties.getProperty(SunORBSubsystemConstants.NAMING_EXPORT_CORBALOC, "on");
            //if (exportCorbalocProperty.equalsIgnoreCase("on")) {
            //    properties.setProperty(SunORBSubsystemConstants.JACORB_NAME_SERVICE_MAP_KEY, rootContext);
            //}

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

        JDKORBLogger.ROOT_LOGGER.corbaORBServiceStarted();
    }

    @Override
    public void stop(StopContext context) {
        JDKORBLogger.ROOT_LOGGER.debugServiceStop(context.getController().getName().getCanonicalName());
        // stop the ORB asynchronously.
        context.asynchronous();
        ORBDestroyer destroyer = new ORBDestroyer(this.orb, context);
        executorInjector.getValue().execute(destroyer);
    }

    @Override
    public ORB getValue() throws IllegalStateException, IllegalArgumentException {
        return this.orb;
    }

    /**
     * <p>
     * Obtains a reference to the SunORB IIOP socket binding injector. This injector is used to inject a {@code ServiceBinding}
     * containing the IIOP socket properties.
     * </p>
     *
     * @return a reference to the {@code Injector<SocketBinding>} used to inject the SunORB IIOP socket properties.
     */
    public Injector<SocketBinding> getSunORBSocketBindingInjector() {
        return this.sunORBSocketBindingInjector;
    }

    /**
     * <p>
     * Obtains a reference to the SunORB IIOP/SSL socket binding injector. This injector is used to inject a
     * {@code ServiceBinding} containing the IIOP/SSL socket properties.
     * </p>
     *
     * @return a reference to the {@code Injector<SocketBinding>} used to inject the SunORB IIOP/SSL socket properties.
     */
    public Injector<SocketBinding> getSunORBSSLSocketBindingInjector() {
        return this.sunORBSSLSocketBindingInjector;
    }

    /**
     * <p>
     * Obtains a reference to the executor service injector. This injector is used to inject a
     * {@link java.util.concurrent.ExecutorService} for use in blocking tasks during startup or shutdown.
     * </p>
     *
     * @return a reference to the {@code Injector<Executor>} used to inject the executor service.
     */
    public InjectedValue<ExecutorService> getExecutorInjector() {
        return executorInjector;
    }

    /**
     * <p>
     * Gets the value of the specified ORB property. All ORB properties can be queried using this method. This includes
     * the properties that have been explicitly set by this service prior to creating the ORB and all SunORB properties
     * that have been specified in the SunORB subsystem configuration.
     * </p>
     *
     * @param key the property key.
     * @return the property value or {@code null} if the property with the specified key hasn't been configured.
     */
    public String getORBProperty(String key) {
        return properties.getProperty(key);
    }

    public static ORB getCurrent() {
        return (ORB) currentServiceContainer().getRequiredService(IIOPServiceNames.ORB_SERVICE_NAME).getValue();
    }

    @Override
    public String sslPort() {
        return getORBProperty(JDKORBSubsystemConstants.ORB_SSL_PORT);
    }

    @Override
    public String orbAddress() {
        return getORBProperty(ORBConstants.SERVER_HOST_PROPERTY);
    }

    @Override
    public boolean interopIONA() {
        return "on".equalsIgnoreCase(getORBProperty(JDKORBSubsystemConstants.INTEROP_IONA));
    }

    @Override
    public Policy createSSLPolicy(boolean sslRequired) throws PolicyError {
        return null;
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
                properties = null;
                this.context.complete();
            }
        }
    }


    private static ServiceContainer currentServiceContainer() {
        return AccessController.doPrivileged(new PrivilegedAction<ServiceContainer>() {
            @Override
            public ServiceContainer run() {
                return CurrentServiceContainer.getServiceContainer();
            }
        });
    }
}
