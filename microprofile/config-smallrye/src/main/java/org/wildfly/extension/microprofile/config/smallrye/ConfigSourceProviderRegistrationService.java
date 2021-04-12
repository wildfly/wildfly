package org.wildfly.extension.microprofile.config.smallrye;

import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.jboss.as.controller.OperationContext;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

public class ConfigSourceProviderRegistrationService implements Service {
    private final String name;
    private ConfigSourceProvider configSourceProvider;
    private final Registry<ConfigSourceProvider> sources;

    ConfigSourceProviderRegistrationService(String name, ConfigSourceProvider configSourceProvider, Registry<ConfigSourceProvider> sources) {
        this.name = name;
        this.configSourceProvider = configSourceProvider;
        this.sources = sources;
    }

    static void install(OperationContext context, String name, ConfigSourceProvider configSourceProvider, Registry registry) {
        context.getServiceTarget()
                .addService(ServiceNames.CONFIG_SOURCE_PROVIDER.append(name))
                .setInstance(new ConfigSourceProviderRegistrationService(name, configSourceProvider, registry))
                .install();
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        sources.register(name, configSourceProvider);
    }

    @Override
    public void stop(StopContext stopContext) {
        sources.unregister(name);
    }
}
