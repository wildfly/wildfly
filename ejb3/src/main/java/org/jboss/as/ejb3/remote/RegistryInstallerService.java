package org.jboss.as.ejb3.remote;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.registry.Registry;

public class RegistryInstallerService implements Service<Void> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb", "remoting", "connector", "client-mappings", "installer");

    @SuppressWarnings("rawtypes")
    private final InjectedValue<RegistryCollector> collector = new InjectedValue<>();
    @SuppressWarnings("rawtypes")
    private final InjectedValue<Registry> registry = new InjectedValue<>();

    public ServiceBuilder<Void> build(ServiceTarget target) {
        return target.addService(SERVICE_NAME, this)
                .addDependency(RegistryCollectorService.SERVICE_NAME, RegistryCollector.class, this.collector)
                .addDependency(ServiceBuilder.DependencyType.OPTIONAL, ServiceName.JBOSS.append("clustering", "registry", "ejb", "default"), Registry.class, this.registry)
        ;
    }

    @Override
    public Void getValue() {
        return null;
    }

    @Override
    public void start(StartContext context) {
        Registry<?, ?> registry = this.registry.getOptionalValue();
        if (registry != null) {
            this.collector.getValue().add(registry);
        }
    }

    @Override
    public void stop(StopContext context) {
        Registry<?, ?> registry = this.registry.getOptionalValue();
        if (registry != null) {
            this.collector.getValue().remove(registry);
        }
    }
}
