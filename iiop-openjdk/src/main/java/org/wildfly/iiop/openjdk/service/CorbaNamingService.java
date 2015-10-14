/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.wildfly.iiop.openjdk.service;

import java.util.Properties;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.PortableServer.POA;
import org.wildfly.iiop.openjdk.Constants;
import org.wildfly.iiop.openjdk.IIOPExtension;
import org.wildfly.iiop.openjdk.logging.IIOPLogger;
import org.wildfly.iiop.openjdk.naming.CorbaNamingContext;

/**
 * <p>
 * This class implements a {@code Service} that provides the default CORBA naming service for JBoss to use.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class CorbaNamingService implements Service<NamingContextExt> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append(IIOPExtension.SUBSYSTEM_NAME, "naming-service");

    private static final Properties properties = new Properties();

    private final InjectedValue<POA> rootPOAInjector = new InjectedValue<POA>();

    private final InjectedValue<POA> namingPOAInjector = new InjectedValue<POA>();

    private final InjectedValue<ORB> orbInjector = new InjectedValue<ORB>();

    private volatile NamingContextExt namingService;

    public CorbaNamingService(Properties props) {
        if (props != null) {
            properties.putAll(props);
        }
    }

    @Override
    public void start(StartContext context) throws StartException {
        IIOPLogger.ROOT_LOGGER.debugf("Starting service %s", context.getController().getName().getCanonicalName());

        ORB orb = orbInjector.getValue();
        POA rootPOA = rootPOAInjector.getValue();
        POA namingPOA = namingPOAInjector.getValue();

        try {
            // initialize the static naming service variables.
            CorbaNamingContext.init(orb, rootPOA);

            // create and initialize the root context instance according to the configuration.
            CorbaNamingContext ns = new CorbaNamingContext();
            ns.init(namingPOA, false, false);

            // create and activate the root context.
            byte[] rootContextId = "root".getBytes();
            namingPOA.activate_object_with_id(rootContextId, ns);
            namingService = NamingContextExtHelper.narrow(namingPOA.create_reference_with_id(rootContextId,
                    "IDL:omg.org/CosNaming/NamingContextExt:1.0"));

            // exporting the NameService initial reference
            ((com.sun.corba.se.impl.orb.ORBImpl) orb).register_initial_reference(Constants.NAME_SERVICE_INIT_REF,
                    namingPOA.servant_to_reference(ns));

            // exporting root-context initial reference
            final boolean exportCorbaloc = properties.getProperty(Constants.NAMING_EXPORT_CORBALOC).equals("true");
            if (exportCorbaloc) {
                final String rootContext = properties.getProperty(Constants.NAMING_ROOT_CONTEXT);
                ((com.sun.corba.se.impl.orb.ORBImpl) orb).register_initial_reference(rootContext,
                        namingPOA.servant_to_reference(ns));
            }

        } catch (Exception e) {
            throw IIOPLogger.ROOT_LOGGER.failedToStartJBossCOSNaming(e);
        }

        // bind the corba naming service to JNDI.
        CorbaServiceUtil.bindObject(context.getChildTarget(), "corbanaming", namingService);

        if (IIOPLogger.ROOT_LOGGER.isDebugEnabled()) {
            IIOPLogger.ROOT_LOGGER.corbaNamingServiceStarted();
            IIOPLogger.ROOT_LOGGER.debugf("Naming: [%s]", orb.object_to_string(namingService));
        }
    }

    @Override
    public void stop(StopContext context) {
        if (IIOPLogger.ROOT_LOGGER.isDebugEnabled()) {
            IIOPLogger.ROOT_LOGGER.debugf("Stopping service %s", context.getController().getName().getCanonicalName());
        }
    }

    @Override
    public NamingContextExt getValue() throws IllegalStateException, IllegalArgumentException {
        return this.namingService;
    }

    /**
     * <p>
     * Obtains a reference to the {@code ORB} injector which allows the injection of the running {@code ORB} that will
     * be used to initialize the naming service.
     * </p>
     *
     * @return the {@code Injector<ORB>} used to inject the running {@code ORB}.
     */
    public Injector<ORB> getORBInjector() {
        return this.orbInjector;
    }

    /**
     * <p>
     * Obtains a reference to the {@code RootPOA} injector which allows the injection of the root {@code POA} that will
     * be used to initialize the naming service.
     * </p>
     *
     * @return the {@code Injector<POA>} used to inject the root {@code POA}.
     */
    public Injector<POA> getRootPOAInjector() {
        return this.rootPOAInjector;
    }

    /**
     * <p>
     * Obtains a reference to the {@code POA} injector which allows the injection of the {@code POA} that will be used
     * activate the naming service.
     * </p>
     *
     * @return the {@code Injector<POA>} used to inject the naming service {@code POA}.
     */
    public Injector<POA> getNamingPOAInjector() {
        return this.namingPOAInjector;
    }
}
