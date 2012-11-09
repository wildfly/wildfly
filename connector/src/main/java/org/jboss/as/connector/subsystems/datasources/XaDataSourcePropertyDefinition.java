package org.jboss.as.connector.subsystems.datasources;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

import static org.jboss.as.connector.subsystems.datasources.Constants.XADATASOURCE_PROPERTIES;

/**
 * @author Stefano Maestri
 */
public class XaDataSourcePropertyDefinition extends SimpleResourceDefinition {
    protected static final PathElement PATH_PROPERTIES = PathElement.pathElement(XADATASOURCE_PROPERTIES.getName());
    static final XaDataSourcePropertyDefinition INSTANCE = new XaDataSourcePropertyDefinition(false);
    static final XaDataSourcePropertyDefinition DEPLOYED_INSTANCE = new XaDataSourcePropertyDefinition(true);

    private final boolean deployed;

    private XaDataSourcePropertyDefinition(final boolean deployed) {
        super(PATH_PROPERTIES,
                DataSourcesExtension.getResourceDescriptionResolver("xa-data-source", "xa-datasource-properties"),
                deployed ? null : XaDataSourcePropertyAdd.INSTANCE,
                deployed ? null : XaDataSourcePropertyRemove.INSTANCE);
        this.deployed = deployed;

    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        if (deployed) {
            SimpleAttributeDefinition runtimeAttribute = new SimpleAttributeDefinitionBuilder(Constants.XADATASOURCE_PROPERTY_VALUE).setFlags(AttributeAccess.Flag.STORAGE_RUNTIME).build();
            resourceRegistration.registerReadOnlyAttribute(runtimeAttribute, XMLXaDataSourceRuntimeHandler.INSTANCE);
        } else {
            resourceRegistration.registerReadOnlyAttribute(Constants.XADATASOURCE_PROPERTY_VALUE, null);
        }
    }

}
