/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.iiop.openjdk.service;

import java.net.InetSocketAddress;
import java.security.AccessController;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.network.ManagedBinding;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.omg.CORBA.ORB;
import org.wildfly.iiop.openjdk.Constants;
import org.wildfly.iiop.openjdk.IIOPExtension;
import org.wildfly.iiop.openjdk.csiv2.CSIV2IORToSocketInfo;
import org.wildfly.iiop.openjdk.logging.IIOPLogger;
import org.wildfly.iiop.openjdk.naming.jndi.CorbaUtils;
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

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append(IIOPExtension.SUBSYSTEM_NAME, "orb-service");

    private static final Properties properties = new Properties();

    private final Consumer<ORB> serviceConsumer;
    private final Supplier<ExecutorService> executorServiceSupplier;
    private final Supplier<SocketBinding> iiopSocketBindingSupplier;
    private final Supplier<SocketBinding> iiopSSLSocketBindingSupplier;

    private volatile ORB orb;

    /**
     * <p>
     * Creates an instance of {@code CorbaORBService} with the specified {@code ORBImplementation} and initializers.
     * </p>
     *
     * @param props a {@code Properties} instance containing the IIOP subsystem configuration properties.
     */
    public CorbaORBService(Properties props, final Consumer<ORB> serviceConsumer, final Supplier<ExecutorService> executorServiceSupplier, final Supplier<SocketBinding> iiopSocketBindingSupplier, final Supplier<SocketBinding> iiopSSLSocketBindingSupplier) {
        if (props != null) {
            properties.putAll(props);
        }
        this.serviceConsumer = serviceConsumer;
        this.executorServiceSupplier = executorServiceSupplier;
        this.iiopSocketBindingSupplier = iiopSocketBindingSupplier;
        this.iiopSSLSocketBindingSupplier = iiopSSLSocketBindingSupplier;
    }

    @Override
    public void start(StartContext context) throws StartException {
        if (IIOPLogger.ROOT_LOGGER.isDebugEnabled()) {
            IIOPLogger.ROOT_LOGGER.debugf("Starting service %s", context.getController().getName().getCanonicalName());
        }

        try {
            // set the ORBClass and ORBSingleton class as system properties.
            properties.setProperty(Constants.ORB_CLASS, ORBImpl.class.getName());
            properties.setProperty(Constants.ORB_SINGLETON_CLASS, ORBSingleton.class.getName());
            WildFlySecurityManager.setPropertyPrivileged(Constants.ORB_CLASS, ORBImpl.class.getName());
            WildFlySecurityManager.setPropertyPrivileged(Constants.ORB_SINGLETON_CLASS, ORBSingleton.class.getName());

            properties.setProperty(ORBConstants.IOR_TO_SOCKET_INFO_CLASS_PROPERTY, CSIV2IORToSocketInfo.class.getName());

            // set the IIOP and IIOP/SSL ports from the respective socket bindings.
            final SocketBinding socketBinding = iiopSocketBindingSupplier.get();
            final SocketBinding sslSocketBinding = iiopSSLSocketBindingSupplier.get();

            if (socketBinding != null) {
                InetSocketAddress address = this.iiopSocketBindingSupplier.get().getSocketAddress();
                properties.setProperty(ORBConstants.SERVER_HOST_PROPERTY, address.getAddress().getHostAddress());
                properties.setProperty(ORBConstants.SERVER_PORT_PROPERTY, String.valueOf(address.getPort()));
                properties.setProperty(ORBConstants.PERSISTENT_SERVER_PORT_PROPERTY, String.valueOf(address.getPort()));

                socketBinding.getSocketBindings().getNamedRegistry().registerBinding(ManagedBinding.Factory.createSimpleManagedBinding(socketBinding));
            }
            if (sslSocketBinding != null) {
                InetSocketAddress address = this.iiopSSLSocketBindingSupplier.get().getSocketAddress();
                properties.setProperty(ORBConstants.SERVER_HOST_PROPERTY, address.getAddress().getHostAddress());
                properties.setProperty(Constants.ORB_SSL_PORT, String.valueOf(address.getPort()));
                final String sslSocket = new StringBuilder().append(Constants.SSL_SOCKET_TYPE).append(':')
                        .append(String.valueOf(address.getPort())).toString();
                properties.setProperty(ORBConstants.LISTEN_SOCKET_PROPERTY, sslSocket);
                if (!properties.containsKey(Constants.ORB_ADDRESS)) {
                    properties.setProperty(Constants.ORB_ADDRESS, address.getAddress().getHostAddress());
                }

                sslSocketBinding.getSocketBindings().getNamedRegistry().registerBinding(ManagedBinding.Factory.createSimpleManagedBinding(sslSocketBinding));
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

        serviceConsumer.accept(this.orb);

        CorbaUtils.setOrbProperties(properties);

        IIOPLogger.ROOT_LOGGER.corbaORBServiceStarted();
    }

    @Override
    public void stop(StopContext context) {
        if (IIOPLogger.ROOT_LOGGER.isDebugEnabled()) {
            IIOPLogger.ROOT_LOGGER.debugf("Stopping service %s", context.getController().getName().getCanonicalName());
        }

        final SocketBinding socketBinding = iiopSocketBindingSupplier.get();
        final SocketBinding sslSocketBinding = iiopSSLSocketBindingSupplier.get();
        if (socketBinding != null) {
            socketBinding.getSocketBindings().getNamedRegistry().unregisterBinding(socketBinding.getName());
        }
        if (sslSocketBinding != null) {
            sslSocketBinding.getSocketBindings().getNamedRegistry().unregisterBinding(sslSocketBinding.getName());
        }
        // stop the ORB asynchronously.
        final ORBDestroyer destroyer = new ORBDestroyer(this.orb, context);
        try {
            executorServiceSupplier.get().execute(destroyer);
        } catch (RejectedExecutionException e) {
            destroyer.run();
        } finally {
            context.asynchronous();
        }
        serviceConsumer.accept(null);
    }

    @Override
    public ORB getValue() throws IllegalStateException, IllegalArgumentException {
        return this.orb;
    }

    /**
     * <p>
     * Gets the value of the specified ORB property. All ORB properties can be queried using this method. This includes
     * the properties that have been explicitly set by this service prior to creating the ORB and all IIOP properties
     * that have been specified in the IIOP subsystem configuration.
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
