package org.jboss.as.ejb3.remote;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.ejb.BeanManagerFactoryBuilderConfiguration;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.spi.CacheServiceNames;

public class RegistryInstallerService implements Service<Void> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb", "remoting", "connector", "client-mappings", "installer");

    @SuppressWarnings("rawtypes")
    private final InjectedValue<RegistryCollector> collector = new InjectedValue<>();
    @SuppressWarnings("rawtypes")
    private final InjectedValue<Registry> registry = new InjectedValue<>();

    public ServiceBuilder<Void> build(ServiceTarget target) {
        return target.addService(SERVICE_NAME, this)
                .addDependency(RegistryCollectorService.SERVICE_NAME, RegistryCollector.class, this.collector)
                .addDependency(CacheServiceNames.REGISTRY.getServiceName(BeanManagerFactoryBuilderConfiguration.DEFAULT_CONTAINER_NAME), Registry.class, this.registry)
        ;
    }

    @Override
    public Void getValue() {
        return null;
    }

    @Override
    public void start(StartContext context) {
        Registry<?, ?> registry = this.registry.getValue();
        if (registry.getGroup().getLocalNode().getSocketAddress() != null) {
            this.collector.getValue().add(registry);
        }
    }

    @Override
    public void stop(StopContext context) {
        Registry<?, ?> registry = this.registry.getValue();
        if (registry.getGroup().getLocalNode().getSocketAddress() != null) {
            this.collector.getValue().remove(registry);
        }
    }
}
