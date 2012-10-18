package org.jboss.as.jaxr.extension;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.jaxr.JAXRConfiguration;
import org.jboss.as.jaxr.ModelConstants;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class JAXRPropertyDefinition extends SimpleResourceDefinition {
    static final SimpleAttributeDefinition VALUE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.VALUE, ModelType.STRING, false)
            .setAllowExpression(true)
            .build();
    private final JAXRConfiguration config;

    JAXRPropertyDefinition(JAXRConfiguration config) {
        super(JAXRExtension.PROPERTY_PATH,
                JAXRExtension.getResolver(ModelConstants.PROPERTY),
                new JAXRPropertyAdd(config),
                new JAXRPropertyRemove(config)
        );

        this.config = config;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        resourceRegistration.registerReadWriteAttribute(VALUE, null, new JAXRPropertyWrite(config, VALUE));
    }
}
