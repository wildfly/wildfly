package org.jboss.as.arquillian.service;

import org.jboss.arquillian.container.test.spi.RemoteLoadableExtension;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

/**
 * @author Stuart Douglas
 */
public class InContainerExtension implements RemoteLoadableExtension {

    @Override
    public void register(LoadableExtension.ExtensionBuilder builder) {
        builder.observer(InContainerManagementClientProvider.class);
        builder.service(ResourceProvider.class, InContainerManagementClientProvider.class);
    }
}
