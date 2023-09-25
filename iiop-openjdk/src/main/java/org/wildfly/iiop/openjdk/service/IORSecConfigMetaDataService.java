/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.iiop.openjdk.service;

import java.security.AccessController;

import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.metadata.ejb.jboss.IORSecurityConfigMetaData;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.iiop.openjdk.IIOPExtension;
import org.wildfly.iiop.openjdk.logging.IIOPLogger;

/**
 * <p>
 * Service that holds the configured {@code IORSecurityConfigMetaData}.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class IORSecConfigMetaDataService implements Service<IORSecurityConfigMetaData> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS
            .append(IIOPExtension.SUBSYSTEM_NAME, "ior-security-config");

    private IORSecurityConfigMetaData iorSecurityConfigMetaData;

    public IORSecConfigMetaDataService(final IORSecurityConfigMetaData metaData) {
        this.iorSecurityConfigMetaData = metaData;
    }

    @Override
    public void start(final StartContext startContext) throws StartException {
        if (IIOPLogger.ROOT_LOGGER.isDebugEnabled()) {
            IIOPLogger.ROOT_LOGGER.debugf("Starting service %s", startContext.getController().getName().getCanonicalName());
        }
    }

    @Override
    public void stop(final StopContext stopContext) {
        if (IIOPLogger.ROOT_LOGGER.isDebugEnabled()) {
            IIOPLogger.ROOT_LOGGER.debugf("Stopping service %s", stopContext.getController().getName().getCanonicalName());
        }
    }

    @Override
    public IORSecurityConfigMetaData getValue() throws IllegalStateException, IllegalArgumentException {
        return this.iorSecurityConfigMetaData;
    }

    public static IORSecurityConfigMetaData getCurrent() {
        return (IORSecurityConfigMetaData) currentServiceContainer().getRequiredService(SERVICE_NAME).getValue();
    }

    private static ServiceContainer currentServiceContainer() {
        if (System.getSecurityManager() == null) {
            return CurrentServiceContainer.getServiceContainer();
        }
        return AccessController.doPrivileged(CurrentServiceContainer.GET_ACTION);
    }
}
