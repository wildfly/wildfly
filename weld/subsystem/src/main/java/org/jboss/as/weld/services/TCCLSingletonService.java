/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.services;

import org.jboss.as.server.Services;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.weld.bootstrap.api.SingletonProvider;

/**
 * Service that manages the weld {@link SingletonProvider}
 *
 * @author Stuart Douglas
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class TCCLSingletonService implements Service {

    public static final ServiceName SERVICE_NAME = Services.JBOSS_AS.append("weld", "singleton");

    @Override
    public void start(final StartContext context) throws StartException {
        SingletonProvider.initialize(new ModuleGroupSingletonProvider());
    }

    @Override
    public void stop(final StopContext context) {
        SingletonProvider.reset();
    }

}
