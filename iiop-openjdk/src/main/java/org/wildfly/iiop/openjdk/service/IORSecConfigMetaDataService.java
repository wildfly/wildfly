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
