package org.wildfly.extension.microprofile.config.smallrye;

import io.smallrye.config.PropertiesConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.as.controller.OperationContext;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;

public class PropertiesConfigSourceRegistrationService implements Service {

    private final String name;
    private final PropertiesConfigSource configSource;
    private final Registry<ConfigSource> sources;

    PropertiesConfigSourceRegistrationService(String name, PropertiesConfigSource configSource, Registry<ConfigSource> sources) {
        this.name = name;
        this.configSource = configSource;
        this.sources = sources;
    }

    static void install(OperationContext context, String name, PropertiesConfigSource configSource, Registry registry) {
        context.getServiceTarget()
                .addService(ServiceNames.CONFIG_SOURCE.append(name))
                .setInstance(new PropertiesConfigSourceRegistrationService(name, configSource, registry))
                .install();
    }

    @Override
    public void start(StartContext startContext) {
        this.sources.register(this.name, configSource);
    }

    @Override
    public void stop(StopContext context) {
        this.sources.unregister(this.name);
    }
}
