package org.jboss.as.test.clustering.cluster.service.bean;

import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.Node;
import org.wildfly.clustering.service.ServiceProviderRegistration;
import org.wildfly.clustering.service.ServiceProviderRegistry;

@Singleton
@Startup
@Local(ServiceProviderRegistration.class)
public class ServiceProviderRegistryBean implements ServiceProviderRegistration, ServiceProviderRegistration.Listener {
    private static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("test", "registry");

    @Resource(lookup = "java:jboss/clustering/registry/ejb")
    private ServiceProviderRegistry registry;
    private ServiceProviderRegistration registration;

    @PostConstruct
    public void init() {
        this.registration = this.registry.createRegistration(SERVICE_NAME, this);
    }

    @PreDestroy
    public void destroy() {
        this.close();
    }

    @Override
    public Set<Node> getServiceProviders() {
        return this.registration.getServiceProviders();
    }

    @Override
    public void close() {
        this.registration.close();
    }

    @Override
    public void serviceProvidersChanged(Set<Node> nodes) {
        System.out.println(String.format("ServiceProviderRegistry.Listener.serviceProvidersChanged(%s)", nodes));
    }
}
