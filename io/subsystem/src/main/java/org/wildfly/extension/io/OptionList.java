package org.wildfly.extension.io;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.xnio.Option;
import org.xnio.OptionMap;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 */
public class OptionList {
    private OptionList() {

    }

    public static OptionMap resolveOptions(final ExpressionResolver context, final ModelNode model, Collection<OptionAttributeDefinition> attributes) throws OperationFailedException {
        OptionMap.Builder builder = OptionMap.builder();
        for (OptionAttributeDefinition attr : attributes) {
            attr.resolveOption(context, model, builder);
        }
        return builder.getMap();
    }


    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private List<OptionAttributeDefinition> attributes = new LinkedList<>();

        private Builder() {

        }

        public Builder addOption(Option<?> option, String name) {
            return addOption(option, name, null, true, true);
        }

        public Builder addOption(Option<?> option, String name, ModelNode defaultValue) {
            return addOption(option, name, defaultValue, true, true);
        }

        public Builder addOption(Option<?> option, String name, ModelNode defaultValue, boolean allowNull) {
            return addOption(option, name, defaultValue, true, allowNull);
        }

        public Builder addOption(Option<?> option, String name, ModelNode defaultValue, boolean allowExpression, boolean allowNull) {
            attributes.add(OptionAttributeDefinition.builder(name, option)
                    .setDefaultValue(defaultValue)
                    .setAllowExpression(allowExpression)
                    .setAllowNull(allowNull)
                    .build()
            );
            return this;
        }

        public List<OptionAttributeDefinition> build() {
            return attributes;
        }
    }


}
