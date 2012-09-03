package org.jboss.as.controller;

import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public final class StringListAttributeDefinition extends PrimitiveListAttributeDefinition {

    private StringListAttributeDefinition(final String name, final String xmlName, final boolean allowNull, final int minSize, final int maxSize, final String[] alternatives,
                                          final String[] requires, ParameterValidator elementValidator, final AttributeMarshaller attributeMarshaller, final boolean resourceOnly,
                                          final DeprecationData deprecated, final AttributeAccess.Flag... flags) {
        super(name, xmlName, allowNull, ModelType.STRING, minSize, maxSize, alternatives, requires, elementValidator, attributeMarshaller, resourceOnly, deprecated, flags);
    }

    public static class Builder extends AbstractAttributeDefinitionBuilder<Builder, StringListAttributeDefinition> {
        public Builder(final String name) {
            super(name, ModelType.STRING);
            validator = new ModelTypeValidator(ModelType.STRING);
        }

        public Builder(final StringListAttributeDefinition basic) {
            super(basic);
            if (validator==null){
                validator = new ModelTypeValidator(ModelType.STRING);
            }
        }

        @Override
        public StringListAttributeDefinition build() {
            return new StringListAttributeDefinition(name, xmlName, allowNull, minSize, maxSize, alternatives, requires, validator, attributeMarshaller, resourceOnly, deprecated, flags);
        }
    }
}
