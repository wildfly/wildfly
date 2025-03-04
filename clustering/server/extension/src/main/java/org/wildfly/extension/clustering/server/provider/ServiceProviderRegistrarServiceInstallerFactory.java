package org.wildfly.extension.clustering.server.provider;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.server.service.BinaryServiceInstallerFactory;
import org.wildfly.subsystem.service.ServiceInstaller;

import java.util.function.BiFunction;

/**
 * Configures a cache or local service provider registry.
 * @author Paul Ferraro
 */
@MetaInfServices(BinaryServiceInstallerFactory.class)
public class ServiceProviderRegistrarServiceInstallerFactory<T> extends AbstractServiceProviderRegistrarServiceInstallerFactory<T>{

    @Override
    public ServiceInstaller apply(CapabilityServiceSupport support, BinaryServiceConfiguration configuration) {
        BiFunction<CapabilityServiceSupport, BinaryServiceConfiguration, ServiceInstaller> factory = configuration.getParentName().equals(ModelDescriptionConstants.LOCAL) ? new LocalServiceProviderRegistrarServiceInstallerFactory<>() : new CacheServiceProviderRegistrarServiceInstallerFactory<>();
        return factory.apply(support, configuration);
    }
}
