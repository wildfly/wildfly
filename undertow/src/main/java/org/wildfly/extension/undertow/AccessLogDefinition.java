package org.wildfly.extension.undertow;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.ValueExpression;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 */
public class AccessLogDefinition extends PersistentResourceDefinition {


    protected static final SimpleAttributeDefinition PATTERN = new SimpleAttributeDefinitionBuilder(Constants.PATTERN, ModelType.STRING, true)
            .setDefaultValue(new ModelNode("common"))
            .setValidator(new StringLengthValidator(1, true))
            .build();
    protected static final SimpleAttributeDefinition WORKER = new SimpleAttributeDefinitionBuilder(Constants.WORKER, ModelType.STRING)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new StringLengthValidator(1))
            .setDefaultValue(new ModelNode("default"))
            .build();
    protected static final SimpleAttributeDefinition PREFIX = new SimpleAttributeDefinitionBuilder(Constants.PREFIX, ModelType.STRING, true)
            .setDefaultValue(new ModelNode("access_log"))
            .setValidator(new StringLengthValidator(1, true))
            .setAllowExpression(true)
            .build();
    protected static final SimpleAttributeDefinition ROTATE = new SimpleAttributeDefinitionBuilder(Constants.ROTATE, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(true))
            .setAllowExpression(true)
            .build();
    protected static final SimpleAttributeDefinition DIRECTORY = new SimpleAttributeDefinitionBuilder(Constants.DIRECTORY, ModelType.STRING)
            .setAllowNull(true)
            .setValidator(new StringLengthValidator(1, true))
            .setDefaultValue(new ModelNode(new ValueExpression("${jboss.server.log.dir}")))
            .setAllowExpression(true)
            .build();
    static final Collection<SimpleAttributeDefinition> ATTRIBUTES = Arrays.asList(
            // IMPORTANT -- keep these in xsd order as this order controls marshalling
            WORKER,
            PATTERN,
            PREFIX,
            ROTATE,
            DIRECTORY
    );
    static final AccessLogDefinition INSTANCE = new AccessLogDefinition();
    private final List<AccessConstraintDefinition> accessConstraints;


    private AccessLogDefinition() {
        super(UndertowExtension.PATH_ACCESS_LOG, UndertowExtension.getResolver(Constants.ACCESS_LOG), new AccessLogAdd(), ReloadRequiredRemoveStepHandler.INSTANCE);
        SensitivityClassification sc = new SensitivityClassification(UndertowExtension.SUBSYSTEM_NAME, "web-access-log", false, false, false);
        this.accessConstraints = new SensitiveTargetAccessConstraintDefinition(sc).wrapAsList();
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return accessConstraints;
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        //noinspection unchecked
        return (Collection) ATTRIBUTES;
    }
}
