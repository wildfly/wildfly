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
package org.jboss.as.weld.services;

import org.jboss.as.server.Services;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.weld.bootstrap.api.SingletonProvider;

/**
 * Service that manages the weld {@link SingletonProvider}
 *
 * @author Stuart Douglas
 *
 */
public class TCCLSingletonService implements Service<TCCLSingletonService> {

    public static final ServiceName SERVICE_NAME = Services.JBOSS_AS.append("weld", "singleton");

    @Override
    public void start(StartContext context) throws StartException {
        SingletonProvider.initialize(new ModuleGroupSingletonProvider());
    }

    @Override
    public void stop(StopContext context) {
        SingletonProvider.reset();
    }

    @Override
    public TCCLSingletonService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

}
