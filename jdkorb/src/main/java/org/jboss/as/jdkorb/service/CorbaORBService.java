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
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import org.jboss.as.jdkorb.JdkORBSubsystemConstants;
import org.jboss.as.jdkorb.csiv2.CSIV2IORToSocketInfo;
import org.jboss.as.jdkorb.logging.JdkORBLogger;
import org.jboss.as.jdkorb.naming.jndi.CorbaUtils;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.omg.CORBA.ORB;
import org.wildfly.security.manager.WildFlySecurityManager;

import com.sun.corba.se.impl.orb.ORBImpl;
import com.sun.corba.se.impl.orb.ORBSingleton;
import com.sun.corba.se.impl.orbutil.ORBConstants;

/**
 * <p>
 * This class implements a {@code Service} that creates and installs a CORBA {@code ORB}.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class CorbaORBService implements Service<ORB> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("jdkorb", "orb-service");

    private static final Properties properties = new Properties();

    private final InjectedValue<ExecutorService> executorInjector = new InjectedValue<ExecutorService>();

    private final InjectedValue<SocketBinding> jdkORBSocketBindingInjector = new InjectedValue<SocketBinding>();

    private final InjectedValue<SocketBinding> jdkORBSSLSocketBindingInjector = new InjectedValue<SocketBinding>();

    private volatile ORB orb;

    /**
     * <p>
     * Creates an instance of {@code CorbaORBService} with the specified {@code ORBImplementation} and initializers.
     * </p>
     *
     * @param props a {@code Properties} instance containing the JdkORB subsystem configuration properties.
     */
    public CorbaORBService(Properties props) {
        if (props != null) {
            properties.putAll(props);
        }
    }

    @Override
    public void start(StartContext context) throws StartException {
        JdkORBLogger.ROOT_LOGGER.debugServiceStartup(context.getController().getName().getCanonicalName());


        try {
            // set the ORBClass and ORBSingleton class as system properties.
            properties.setProperty(JdkORBSubsystemConstants.ORB_CLASS, ORBImpl.class.getName());
            properties.setProperty(JdkORBSubsystemConstants.ORB_SINGLETON_CLASS, ORBSingleton.class.getName());
            SecurityActions.setSystemProperty(JdkORBSubsystemConstants.ORB_CLASS, ORBImpl.class.getName());
            SecurityActions.setSystemProperty(JdkORBSubsystemConstants.ORB_SINGLETON_CLASS, ORBSingleton.class.getName());

            properties.setProperty(ORBConstants.IOR_TO_SOCKET_INFO_CLASS_PROPERTY, CSIV2IORToSocketInfo.class.getName());

            // set the JdkORB IIOP and IIOP/SSL ports from the respective socket bindings.
            if (this.jdkORBSocketBindingInjector.getValue()!= null) {
                InetSocketAddress address = this.jdkORBSocketBindingInjector.getValue().getSocketAddress();
                properties.setProperty(ORBConstants.SERVER_HOST_PROPERTY, address.getAddress().getHostAddress());
                properties.setProperty(ORBConstants.SERVER_PORT_PROPERTY, String.valueOf(address.getPort()));
                properties.setProperty(ORBConstants.PERSISTENT_SERVER_PORT_PROPERTY, String.valueOf(address.getPort()));
            }
            if (this.jdkORBSSLSocketBindingInjector.getValue() != null) {
                InetSocketAddress address = this.jdkORBSSLSocketBindingInjector.getValue().getSocketAddress();
                properties.setProperty(JdkORBSubsystemConstants.ORB_SSL_PORT, String.valueOf(address.getPort()));
                final String sslSocket = new StringBuilder().append(JdkORBSubsystemConstants.SSL_SOCKET_TYPE).append(":")
                        .append(String.valueOf(address.getPort())).toString();
                properties.setProperty(ORBConstants.LISTEN_SOCKET_PROPERTY, sslSocket);
                if (!properties.containsKey(JdkORBSubsystemConstants.ORB_ADDRESS)) {
                    properties.setProperty(JdkORBSubsystemConstants.ORB_ADDRESS, address.getAddress().getHostAddress());
                }
            }

            // initialize the ORB - the thread context classloader needs to be adjusted as the ORB classes are loaded via reflection.
            ClassLoader loader = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
            try {
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(WildFlySecurityManager.getClassLoaderPrivileged(this.getClass()));
                this.orb = ORB.init(new String[0], properties);
                // initialize the ORBSingleton.
                ORB.init();
            } finally {
                // restore the thread context classloader.
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(loader);
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

        JdkORBLogger.ROOT_LOGGER.corbaORBServiceStarted();
    }

    @Override
    public void stop(StopContext context) {
        JdkORBLogger.ROOT_LOGGER.debugServiceStop(context.getController().getName().getCanonicalName());
        // stop the ORB asynchronously.
        final ORBDestroyer destroyer = new ORBDestroyer(this.orb, context);
        try {
            executorInjector.getValue().execute(destroyer);
        } catch (RejectedExecutionException e) {
            destroyer.run();
        } finally {
            context.asynchronous();
        }
    }

    @Override
    public ORB getValue() throws IllegalStateException, IllegalArgumentException {
        return this.orb;
    }

    /**
     * <p>
     * Obtains a reference to the JdkORB IIOP socket binding injector. This injector is used to inject a {@code ServiceBinding}
     * containing the IIOP socket properties.
     * </p>
     *
     * @return a reference to the {@code Injector<SocketBinding>} used to inject the JdkORB IIOP socket properties.
     */
    public Injector<SocketBinding> getJdkORBSocketBindingInjector() {
        return this.jdkORBSocketBindingInjector;
    }

    /**
     * <p>
     * Obtains a reference to the JdkORB IIOP/SSL socket binding injector. This injector is used to inject a
     * {@code ServiceBinding} containing the IIOP/SSL socket properties.
     * </p>
     *
     * @return a reference to the {@code Injector<SocketBinding>} used to inject the JdkORB IIOP/SSL socket properties.
     */
    public Injector<SocketBinding> getJdkORBSSLSocketBindingInjector() {
        return this.jdkORBSSLSocketBindingInjector;
    }

    /**
     * <p>
     * Obtains a reference to the executor service injector. This injector is used to inject a
     * {@link ExecutorService} for use in blocking tasks during startup or shutdown.
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
     * the properties that have been explicitly set by this service prior to creating the ORB and all JdkORB properties
     * that have been specified in the JdkORB subsystem configuration.
     * </p>
     *
     * @param key the property key.
     * @return the property value or {@code null} if the property with the specified key hasn't been configured.
     */
    public static String getORBProperty(String key) {
        return properties.getProperty(key);
    }

    public static ORB getCurrent() {
        return (ORB) currentServiceContainer().getRequiredService(SERVICE_NAME).getValue();
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


    private static ServiceContainer currentServiceContainer() {
        if(System.getSecurityManager() == null) {
            return CurrentServiceContainer.getServiceContainer();
        }
        return AccessController.doPrivileged(CurrentServiceContainer.GET_ACTION);
    }
}
