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
 *
 *
 * @author Stuart Douglas
 */
public class EjbNameRegexService implements Service<EjbNameRegexService> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "ejbNameRegex");

    private volatile boolean ejbNameRegexAllowed;

    public EjbNameRegexService(final boolean defaultDistinctName) {
        this.ejbNameRegexAllowed = defaultDistinctName;
    }

    @Override
    public void start(final StartContext context) throws StartException {

    }

    @Override
    public void stop(final StopContext context) {

    }

    @Override
    public EjbNameRegexService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public boolean isEjbNameRegexAllowed() {
        return ejbNameRegexAllowed;
    }

    public void setEjbNameRegexAllowed(final boolean ejbNameRegexAllowed) {
        this.ejbNameRegexAllowed = ejbNameRegexAllowed;
    }
}
