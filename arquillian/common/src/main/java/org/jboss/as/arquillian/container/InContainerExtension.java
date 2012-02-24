package org.jboss.as.arquillian.container;

import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

/**
 * @author Stuart Douglas
 */
public class InContainerExtension implements LoadableExtension {

    @Override
    public void register(final ExtensionBuilder builder) {
        builder.service(ResourceProvider.class, InContainerManagementClientProvider.class);
        builder.observer(InContainerManagementClientProvider.class);
    }

}
