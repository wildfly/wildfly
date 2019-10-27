package org.jboss.as.test.clustering.cluster.provider.bean;

import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.logging.Logger;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.provider.ServiceProviderRegistration;
import org.wildfly.clustering.provider.ServiceProviderRegistry;

@Singleton
@Startup
@Local(ServiceProviderRegistration.class)
public class ServiceProviderRegistrationBean implements ServiceProviderRegistration<String>, ServiceProviderRegistration.Listener {
    static final Logger log = Logger.getLogger(ServiceProviderRegistrationBean.class);

    @Resource(name = "clustering/providers")
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
        try {
            // Ensure the thread context classloader of the notification is correct
            Thread.currentThread().getContextClassLoader().loadClass(this.getClass().getName());
            // Ensure the correct naming context is set
            Context context = new InitialContext();
            try {
                context.lookup("java:comp/env/clustering/providers");
            } finally {
                context.close();
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        } catch (NamingException e) {
            throw new IllegalStateException(e);
        }
        log.info(String.format("ProviderRegistration.Listener.providersChanged(%s)", nodes));
    }
}
