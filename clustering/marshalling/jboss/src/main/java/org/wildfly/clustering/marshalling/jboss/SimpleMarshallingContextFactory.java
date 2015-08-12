package org.wildfly.clustering.marshalling.jboss;

import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;

/**
 * Factory for creating a {@link SimpleMarshallingContext}.
 * @author Paul Ferraro
 */
public class SimpleMarshallingContextFactory implements MarshallingContextFactory {
    private final MarshallerFactory factory;

    public SimpleMarshallingContextFactory() {
        this(Marshalling.getMarshallerFactory("river", Marshalling.class.getClassLoader()));
    }

    public SimpleMarshallingContextFactory(MarshallerFactory factory) {
        this.factory = factory;
    }

    @Override
    public SimpleMarshallingContext createMarshallingContext(MarshallingConfigurationRepository repository, ClassLoader loader) {
        return new SimpleMarshallingContext(this.factory, repository, loader);
    }
}
