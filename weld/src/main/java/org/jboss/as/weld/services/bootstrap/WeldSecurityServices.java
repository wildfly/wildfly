/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.weld.services.bootstrap;

import java.security.Principal;

import org.jboss.as.security.service.SimpleSecurityManager;
import org.jboss.as.weld.WeldMessages;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.weld.security.spi.SecurityServices;

public class WeldSecurityServices implements Service<WeldSecurityServices>, SecurityServices {

    public static final ServiceName SERVICE_NAME = ServiceName.of("WeldSecurityServices");

    private final InjectedValue<SimpleSecurityManager> securityManagerValue = new InjectedValue<SimpleSecurityManager>();

    @Override
    public void start(StartContext context) throws StartException {

    }

    @Override
    public void stop(StopContext context) {

    }

    @Override
    public WeldSecurityServices getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public Principal getPrincipal() {
        final SimpleSecurityManager securityManager = securityManagerValue.getOptionalValue();
        if (securityManager == null)
            throw WeldMessages.MESSAGES.securityNotEnabled();
        return securityManager.getCallerPrincipal();
    }

    @Override
    public void cleanup() {
    }

    public InjectedValue<SimpleSecurityManager> getSecurityManagerValue() {
        return securityManagerValue;
    }
}
