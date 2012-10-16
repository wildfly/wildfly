package org.jboss.as.security;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class LoginModulesDefinition extends SimpleResourceDefinition {
    static final SimpleAttributeDefinition CODE = new SimpleAttributeDefinitionBuilder(Constants.CODE, ModelType.STRING)
            .setAllowNull(false)
            .setMinSize(1)
            .build();

    static final SimpleAttributeDefinition FLAG = new SimpleAttributeDefinitionBuilder(Constants.FLAG, ModelType.STRING)
            .setAllowNull(false)
            .setValidator(new EnumValidator<ModuleFlag>(ModuleFlag.class, false, false))
            .build();

    static final SimpleAttributeDefinition MODULE = new SimpleAttributeDefinitionBuilder(Constants.MODULE, ModelType.STRING)
            .setAllowNull(true)
            .setMinSize(1)
            .build();
    static final PropertiesAttributeDefinition MODULE_OPTIONS = new PropertiesAttributeDefinition.Builder(Constants.MODULE_OPTIONS, true)
            .build();

    static final AttributeDefinition[] ATTRIBUTES = {CODE, FLAG, MODULE, MODULE_OPTIONS};

    static final LoginModulesDefinition INSTANCE = new LoginModulesDefinition();

    private LoginModulesDefinition() {
        super(PathElement.pathElement(Constants.LOGIN_MODULE),
                SecurityExtension.getResourceDescriptionResolver(Constants.LOGIN_MODULES),
                new LoginModuleAdd(),
                new SecurityDomainReloadRemoveHandler()
        );
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        for (AttributeDefinition attribute : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attribute, null, new SecurityDomainReloadWriteHandler(attribute));
        }
    }

    private static class LoginModuleAdd extends AbstractAddStepHandler {
        @Override
        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
            for (AttributeDefinition attribute : ATTRIBUTES) {
                attribute.validateAndSet(operation, model);
            }
        }
    }
}
