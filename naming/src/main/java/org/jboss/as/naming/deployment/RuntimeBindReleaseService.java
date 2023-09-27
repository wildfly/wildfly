/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.naming.deployment;

import org.jboss.as.naming.logging.NamingLogger;
import org.jboss.as.naming.service.BinderService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link Service} which on stop releases runtime installed {@link org.jboss.as.naming.service.BinderService}s.
 *
 * @author Eduardo Martins
 *
 */
public class RuntimeBindReleaseService implements Service<RuntimeBindReleaseService.References> {

    private final References references = new References();

    @Override
    public References getValue() throws IllegalStateException, IllegalArgumentException {
        return references;
    }

    @Override
    public void start(StartContext context) throws StartException {

    }

    @Override
    public void stop(StopContext context) {
        references.releaseAll();
    }

    public static class References {

        // List instead of Set because binder services use a counter to track its references, which means that for instance, N rebinds will increase the binder service ref counter N times, thus the related BinderServices should have N entries too
        private volatile List<BinderService> services;

        private References() {

        }

        public void add(BinderService service) {
            synchronized (this) {
                if (services == null) {
                    services = new ArrayList<BinderService>();
                }
                services.add(service);
            }
        }

        public boolean contains(ServiceName serviceName) {
            synchronized (this) {
                if (services != null) {
                    for (BinderService service : services) {
                        if (serviceName.equals(service.getServiceName())) {
                            return true;
                        }
                    }
                }
                return false;
            }
        }

        public void releaseAll() {
            synchronized (this) {
                if (services != null) {
                    for (BinderService service : services) {
                        try {
                            service.release();
                        } catch (Throwable e) {
                            NamingLogger.ROOT_LOGGER.failedToReleaseBinderService(e);
                        }
                    }
                    services = null;
                }
            }
        }

    }

}
