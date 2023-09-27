/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
