/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.security.service;

import org.jboss.as.core.security.ServerSecurityManager;
import org.jboss.as.security.SecurityExtension;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.picketbox.plugins.SecurityFactorySecurityManagement;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @author Anil Saldhana
 */
public class SimpleSecurityManagerService implements Service<ServerSecurityManager> {
    public static final ServiceName SERVICE_NAME = SecurityExtension.JBOSS_SECURITY.append("simple-security-manager");

    private final SimpleSecurityManager securityManager = new SimpleSecurityManager();

    @Override
    public void start(StartContext context) throws StartException {
        securityManager.setSecurityManagement(new SecurityFactorySecurityManagement());
    }

    @Override
    public void stop(StopContext context) {
    }

    @Override
    public ServerSecurityManager getValue() throws IllegalStateException, IllegalArgumentException {
        return securityManager;
    }

}
