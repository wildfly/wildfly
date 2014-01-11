package org.jboss.as.server.deployment.service;

import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceNotFoundException;
import org.jboss.msc.service.ServiceRegistry;

import java.security.AccessController;
import java.util.List;

/**
 *
 * TODO: these checks should be part of MSC
 * @author Stuart Douglas
 */
public class SecuredServiceRegistry implements ServiceRegistry {

    public static final RuntimePermission PERMISSION = new RuntimePermission("org.jboss.as.server.deployment.service.USE_SERVICE_REGISTRY");

    private final ServiceRegistry delegate;

    public SecuredServiceRegistry(ServiceRegistry delegate) {
        this.delegate = delegate;
    }

    @Override
    public ServiceController<?> getRequiredService(ServiceName serviceName) throws ServiceNotFoundException {
        AccessController.checkPermission(PERMISSION);
        return delegate.getRequiredService(serviceName);
    }

    @Override
    public ServiceController<?> getService(ServiceName serviceName) {
        AccessController.checkPermission(PERMISSION);
        return delegate.getService(serviceName);
    }

    @Override
    public List<ServiceName> getServiceNames() {
        AccessController.checkPermission(PERMISSION);
        return delegate.getServiceNames();
    }
}
