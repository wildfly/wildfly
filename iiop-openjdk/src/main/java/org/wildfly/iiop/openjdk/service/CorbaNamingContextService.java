/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.iiop.openjdk.service;

import java.nio.charset.StandardCharsets;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;
import org.wildfly.iiop.openjdk.IIOPExtension;
import org.wildfly.iiop.openjdk.logging.IIOPLogger;
import org.wildfly.iiop.openjdk.naming.CorbaNamingContext;
import org.wildfly.security.auth.server.SecurityDomain;

public class CorbaNamingContextService implements Service<CorbaNamingContext> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append(IIOPExtension.SUBSYSTEM_NAME, "naming-context-service");

    private final InjectedValue<POA> rootPOAInjector = new InjectedValue<POA>();

    private final InjectedValue<POA> namingPOAInjector = new InjectedValue<POA>();

    private final InjectedValue<ORB> orbInjector = new InjectedValue<ORB>();

    private final InjectedValue<SecurityDomain> securityDomainInjector = new InjectedValue<SecurityDomain>();

    private volatile CorbaNamingContext namingContext;

    @Override
    public void start(StartContext context) throws StartException {
        IIOPLogger.ROOT_LOGGER.debugf("Starting service %s", context.getController().getName().getCanonicalName());

        ORB orb = orbInjector.getValue();
        POA rootPOA = rootPOAInjector.getValue();
        POA namingPOA = namingPOAInjector.getValue();
        SecurityDomain securityDomain = securityDomainInjector.getOptionalValue();

        try {
            CorbaNamingContext.init(orb, rootPOA);
            if (securityDomain != null) {
                CorbaNamingContext.setSecurityDomain(securityDomain);
            }

            CorbaNamingContext ns = new CorbaNamingContext();
            ns.init(namingPOA, false, false);

            byte[] rootContextId = "root".getBytes(StandardCharsets.UTF_8);
            namingPOA.activate_object_with_id(rootContextId, ns);

            namingContext = ns;
        } catch (Exception e) {
            throw IIOPLogger.ROOT_LOGGER.failedToStartJBossCOSNaming(e);
        }
    }

    @Override
    public void stop(StopContext context) {
        if (IIOPLogger.ROOT_LOGGER.isDebugEnabled()) {
            IIOPLogger.ROOT_LOGGER.debugf("Stopping service %s", context.getController().getName().getCanonicalName());
        }
    }

    @Override
    public CorbaNamingContext getValue() throws IllegalStateException, IllegalArgumentException {
        return this.namingContext;
    }

    public Injector<ORB> getORBInjector() {
        return this.orbInjector;
    }

    public Injector<POA> getRootPOAInjector() {
        return this.rootPOAInjector;
    }

    public Injector<POA> getNamingPOAInjector() {
        return this.namingPOAInjector;
    }

    public Injector<SecurityDomain> getSecurityDomainInjector() {
        return this.securityDomainInjector;
    }
}
