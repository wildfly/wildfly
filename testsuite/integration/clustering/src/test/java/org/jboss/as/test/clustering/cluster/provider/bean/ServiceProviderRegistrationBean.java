package org.jboss.as.test.clustering.cluster.provider.bean;

import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.provider.ServiceProviderRegistration;
import org.wildfly.clustering.provider.ServiceProviderRegistrationFactory;

@Singleton
@Startup
@Local(ServiceProviderRegistration.class)
public class ServiceProviderRegistrationBean implements ServiceProviderRegistration, ServiceProviderRegistration.Listener {
    @Resource(lookup = "java:jboss/clustering/providers/server/default")
    private ServiceProviderRegistrationFactory factory;
    private ServiceProviderRegistration registration;

    @PostConstruct
    public void init() {
        this.registration = this.factory.createRegistration("ServiceProviderRegistrationTestCase", this);
    }

    @PreDestroy
    public void destroy() {
        this.close();
    }

    @Override
    public Object getService() {
        return this.registration.getService();
    }

    @Override
    public Set<Node> getProviders() {
        return this.registration.getProviders();
    }

    @Override
    public void close() {
        this.registration.close();
    }

    @Override
    public void providersChanged(Set<Node> nodes) {
        System.out.println(String.format("ProviderRegistration.Listener.providersChanged(%s)", nodes));
    }
}
