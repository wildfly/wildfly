package org.jboss.as.test.clustering.cluster.provider.bean;

import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.jboss.logging.Logger;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.provider.ServiceProviderRegistration;
import org.wildfly.clustering.provider.ServiceProviderRegistry;

@Singleton
@Startup
@Local(ServiceProviderRegistration.class)
public class ServiceProviderRegistrationBean implements ServiceProviderRegistration<String>, ServiceProviderRegistration.Listener {
    static final Logger log = Logger.getLogger(ServiceProviderRegistrationBean.class);

    @Resource(lookup = "java:jboss/clustering/providers/server/default")
    private ServiceProviderRegistry<String> factory;
    private ServiceProviderRegistration<String> registration;

    @PostConstruct
    public void init() {
        this.registration = this.factory.register("ServiceProviderRegistrationTestCase", this);
    }

    @PreDestroy
    public void destroy() {
        this.close();
    }

    @Override
    public String getService() {
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
        log.info(String.format("ProviderRegistration.Listener.providersChanged(%s)", nodes));
    }
}
