/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.naming;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.naming.service.NamingService;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;

/**
 * Service responsible for providing the {@link InitialContext} as OSGi service.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 29-Oct-2010
 */
public class InitialContextFactoryService extends AbstractService<InitialContext> {

    // The 'jbosgi' prefix followed by the FQN of the service interface allows the OSGi layer
    // to find the service using context.getServiceReference(InitialContext.class.getName())
    public static final ServiceName SERVICE_NAME = ServiceName.of("jbosgi", "xservice", InitialContext.class.getName());

    private InjectedValue<NamingStore> injectedNamingStore = new InjectedValue<NamingStore>();

    public static ServiceController<InitialContext> addService(final ServiceTarget target, final ServiceVerificationHandler verificationHandler) {
        InitialContextFactoryService service = new InitialContextFactoryService();
        ServiceBuilder<InitialContext> serviceBuilder = target.addService(SERVICE_NAME, service);
        serviceBuilder.addDependency(NamingService.SERVICE_NAME, NamingStore.class, service.injectedNamingStore);
        serviceBuilder.addListener(verificationHandler);
        return serviceBuilder.install();
    }

    @Override
    public InitialContext getValue() throws IllegalStateException {
        try {
            return new InitialContext();
        } catch (NamingException ex) {
            throw new IllegalStateException("Cannot obtain InitialContext", ex);
        }
    }
}
