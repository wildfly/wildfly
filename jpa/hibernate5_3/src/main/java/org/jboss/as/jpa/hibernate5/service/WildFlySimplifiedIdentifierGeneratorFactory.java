package org.jboss.as.jpa.hibernate5.service;


import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.id.factory.internal.DefaultIdentifierGeneratorFactory;
import org.hibernate.id.factory.spi.MutableIdentifierGeneratorFactory;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import java.util.Map;

final class WildFlySimplifiedIdentifierGeneratorFactory extends DefaultIdentifierGeneratorFactory {

    public static class Initiator implements StandardServiceInitiator<MutableIdentifierGeneratorFactory> {
        @Override
        public MutableIdentifierGeneratorFactory initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
            return new WildFlySimplifiedIdentifierGeneratorFactory();
        }

        @Override
        public Class<MutableIdentifierGeneratorFactory> getServiceInitiated() {
            return MutableIdentifierGeneratorFactory.class;
        }
    }

    public WildFlySimplifiedIdentifierGeneratorFactory() {
        super( true /* ignore the bean manager */ );
    }

}