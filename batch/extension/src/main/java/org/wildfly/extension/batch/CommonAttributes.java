package org.wildfly.extension.batch;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
interface CommonAttributes {

    SimpleAttributeDefinition VALUE = SimpleAttributeDefinitionBuilder.create("value", ModelType.STRING, false)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, false, true))
                    .build();
}
