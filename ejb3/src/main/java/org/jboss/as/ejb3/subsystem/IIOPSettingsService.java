/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.subsystem;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Services that manages IIOP settings
 * @author Stuart Douglas
 */
public class IIOPSettingsService implements Service<IIOPSettingsService> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "iiop", "settingsService");

    private volatile boolean enabledByDefault = false;
    private volatile boolean useQualifiedName = false;

    public IIOPSettingsService(final boolean enabledByDefault, final boolean useQualifiedName) {
        this.enabledByDefault = enabledByDefault;
        this.useQualifiedName = useQualifiedName;
    }

    @Override
    public void start(final StartContext context) throws StartException {

    }

    @Override
    public void stop(final StopContext context) {

    }

    @Override
    public IIOPSettingsService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public boolean isEnabledByDefault() {
        return enabledByDefault;
    }

    public void setEnabledByDefault(final boolean enabledByDefault) {
        this.enabledByDefault = enabledByDefault;
    }

    public boolean isUseQualifiedName() {
        return useQualifiedName;
    }

    public void setUseQualifiedName(final boolean useQualifiedName) {
        this.useQualifiedName = useQualifiedName;
    }
}
