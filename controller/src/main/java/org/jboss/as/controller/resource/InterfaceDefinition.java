package org.jboss.as.controller.resource;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.descriptions.common.InterfaceDescription;
import org.jboss.as.controller.operations.common.InterfaceCriteriaWriteHandler;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.parsing.Element;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class InterfaceDefinition extends SimpleResourceDefinition {
    public static final InterfaceDefinition INSTANCE = new InterfaceDefinition();


    public static final String[] ALTERNATIVES_ANY = new String[]{ModelDescriptionConstants.ANY_ADDRESS, ModelDescriptionConstants.ANY_IPV4_ADDRESS, ModelDescriptionConstants.ANY_IPV6_ADDRESS};
    public static final String[] OTHERS = new String[]{localName(Element.INET_ADDRESS), localName(Element.LINK_LOCAL_ADDRESS),
            localName(Element.LOOPBACK), localName(Element.LOOPBACK_ADDRESS), localName(Element.MULTICAST), localName(Element.NIC),
            localName(Element.NIC_MATCH), localName(Element.POINT_TO_POINT), localName(Element.PUBLIC_ADDRESS), localName(Element.SITE_LOCAL_ADDRESS),
            localName(Element.SUBNET_MATCH), localName(Element.UP), localName(Element.VIRTUAL),
            localName(Element.ANY), localName(Element.NOT)
    };


    public static final AttributeDefinition NAME = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.NAME, ModelType.STRING)
            .build();
    public static final AttributeDefinition ANY_ADDRESS = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.ANY_ADDRESS, ModelType.BOOLEAN)
            .setAllowExpression(false).setAllowNull(true).setRestartAllServices()
            .setValidator(new ModelTypeValidator(ModelType.BOOLEAN, true, false))
            .addAlternatives(OTHERS).addAlternatives(ModelDescriptionConstants.ANY_IPV4_ADDRESS, ModelDescriptionConstants.ANY_IPV6_ADDRESS)
            .build();
    public static final AttributeDefinition ANY_IPV4_ADDRESS = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.ANY_IPV4_ADDRESS, ModelType.BOOLEAN)
            .setAllowExpression(false).setAllowNull(true).setRestartAllServices()
            .setValidator(new ModelTypeValidator(ModelType.BOOLEAN, true, false))
            .addAlternatives(OTHERS).addAlternatives(ModelDescriptionConstants.ANY_ADDRESS, ModelDescriptionConstants.ANY_IPV6_ADDRESS)
            .build();
    /**
     * All other attribute names.
     */

    public static final AttributeDefinition ANY_IPV6_ADDRESS = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.ANY_IPV6_ADDRESS, ModelType.BOOLEAN)
            .setAllowExpression(false).setAllowNull(true).setRestartAllServices()
            .setValidator(new ModelTypeValidator(ModelType.BOOLEAN, true, false))
            .addAlternatives(OTHERS).addAlternatives(ModelDescriptionConstants.ANY_ADDRESS, ModelDescriptionConstants.ANY_IPV4_ADDRESS)
            .build();
    public static final AttributeDefinition INET_ADDRESS = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.INET_ADDRESS, ModelType.STRING)
            .setAllowExpression(true).setAllowNull(true).addAlternatives(ALTERNATIVES_ANY).setRestartAllServices()
            .build();
    public static final AttributeDefinition LINK_LOCAL_ADDRESS = SimpleAttributeDefinitionBuilder.create(localName(Element.LINK_LOCAL_ADDRESS), ModelType.BOOLEAN)
            .setAllowExpression(false).setAllowNull(true).addAlternatives(ALTERNATIVES_ANY).setRestartAllServices()
            .build();
    public static final AttributeDefinition LOOPBACK = SimpleAttributeDefinitionBuilder.create(localName(Element.LOOPBACK), ModelType.BOOLEAN)
            .setAllowExpression(false).setAllowNull(true).addAlternatives(ALTERNATIVES_ANY).setRestartAllServices()
            .build();
    public static final AttributeDefinition LOOPBACK_ADDRESS = SimpleAttributeDefinitionBuilder.create(localName(Element.LOOPBACK_ADDRESS), ModelType.STRING)
            .setAllowExpression(true).setAllowNull(true).addAlternatives(ALTERNATIVES_ANY).setRestartAllServices()
            .build();
    public static final AttributeDefinition NIC = SimpleAttributeDefinitionBuilder.create(localName(Element.NIC), ModelType.STRING)
            .setAllowExpression(true).setAllowNull(true).addAlternatives(ALTERNATIVES_ANY).setRestartAllServices()
            .build();
    public static final AttributeDefinition NIC_MATCH = SimpleAttributeDefinitionBuilder.create(localName(Element.NIC_MATCH), ModelType.STRING)
            .setAllowExpression(true).setAllowNull(true).addAlternatives(ALTERNATIVES_ANY).setRestartAllServices()
            .build();
    public static final AttributeDefinition MULTICAST = SimpleAttributeDefinitionBuilder.create(localName(Element.MULTICAST), ModelType.BOOLEAN)
            .setAllowExpression(false).setAllowNull(true).addAlternatives(ALTERNATIVES_ANY).setRestartAllServices()
            .build();
    public static final AttributeDefinition POINT_TO_POINT = SimpleAttributeDefinitionBuilder.create(localName(Element.POINT_TO_POINT), ModelType.BOOLEAN)
            .setAllowExpression(false).setAllowNull(true).addAlternatives(ALTERNATIVES_ANY).setRestartAllServices()
            .build();
    public static final AttributeDefinition PUBLIC_ADDRESS = SimpleAttributeDefinitionBuilder.create(localName(Element.PUBLIC_ADDRESS), ModelType.BOOLEAN)
            .setAllowExpression(false).setAllowNull(true).addAlternatives(ALTERNATIVES_ANY).setRestartAllServices()
            .build();
    public static final AttributeDefinition SITE_LOCAL_ADDRESS = SimpleAttributeDefinitionBuilder.create(localName(Element.SITE_LOCAL_ADDRESS), ModelType.BOOLEAN)
            .setAllowExpression(false).setAllowNull(true).addAlternatives(ALTERNATIVES_ANY).setRestartAllServices()
            .build();
    public static final AttributeDefinition SUBNET_MATCH = SimpleAttributeDefinitionBuilder.create(localName(Element.SUBNET_MATCH), ModelType.STRING)
            .setAllowExpression(true).setAllowNull(true).addAlternatives(ALTERNATIVES_ANY).setRestartAllServices()
            .build();
    public static final AttributeDefinition UP = SimpleAttributeDefinitionBuilder.create(localName(Element.UP), ModelType.BOOLEAN)
            .setAllowExpression(false).setAllowNull(true).addAlternatives(ALTERNATIVES_ANY).setRestartAllServices()
            .build();
    /**
     * The any-* alternatives.
     */

    public static final AttributeDefinition VIRTUAL = SimpleAttributeDefinitionBuilder.create(localName(Element.VIRTUAL), ModelType.BOOLEAN)
            .setAllowExpression(false).setAllowNull(true).addAlternatives(ALTERNATIVES_ANY).setRestartAllServices()
            .build();
    public static final AttributeDefinition NOT = InterfaceDescription.createNestedComplexType("not");
    public static final AttributeDefinition ANY = InterfaceDescription.createNestedComplexType("any");
    /**
     * The root attributes.
     */
    public static final AttributeDefinition[] ROOT_ATTRIBUTES = new AttributeDefinition[]{

            ANY_ADDRESS, ANY_IPV4_ADDRESS, ANY_IPV6_ADDRESS, INET_ADDRESS, LINK_LOCAL_ADDRESS,
            LOOPBACK, LOOPBACK_ADDRESS, MULTICAST, NIC, NIC_MATCH, POINT_TO_POINT, PUBLIC_ADDRESS,
            SITE_LOCAL_ADDRESS, SUBNET_MATCH, UP, VIRTUAL, ANY, NOT

    };
    /**
     * The nested attributes for any, not.
     */
    public static final AttributeDefinition[] NESTED_ATTRIBUTES = new AttributeDefinition[]{
            INET_ADDRESS, LINK_LOCAL_ADDRESS, LOOPBACK, LOOPBACK_ADDRESS, MULTICAST, NIC,
            NIC_MATCH, POINT_TO_POINT, PUBLIC_ADDRESS, SITE_LOCAL_ADDRESS, SUBNET_MATCH, UP, VIRTUAL
    };
    public static final Set<AttributeDefinition> NESTED_LIST_ATTRIBUTES = new HashSet<AttributeDefinition>(
            Arrays.asList(INET_ADDRESS, NIC, NIC_MATCH, SUBNET_MATCH)
    );
    /**
     * The wildcard criteria attributes
     */
    public static final AttributeDefinition[] WILDCARD_ATTRIBUTES = new AttributeDefinition[]{ANY_ADDRESS, ANY_IPV4_ADDRESS, ANY_IPV6_ADDRESS};
    public static final AttributeDefinition[] SIMPLE_ATTRIBUTES = new AttributeDefinition[]{LINK_LOCAL_ADDRESS, LOOPBACK,
            MULTICAST, POINT_TO_POINT, PUBLIC_ADDRESS, SITE_LOCAL_ADDRESS, UP, VIRTUAL};

    private InterfaceDefinition() {
        super(PathElement.pathElement(INTERFACE),
                CommonDescriptions.getResourceDescriptionResolver());
    }

    public static String localName(final Element element) {
        return element.getLocalName();
    }

    @Override
    public void registerOperations(ManagementResourceRegistration interfaces) {
        super.registerOperations(interfaces);
        InterfaceCriteriaWriteHandler.UPDATE_RUNTIME.register(interfaces);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        super.registerAttributes(registration);
        for (final AttributeDefinition def : InterfaceDefinition.ROOT_ATTRIBUTES) {
            registration.registerReadWriteAttribute(def, null, InterfaceCriteriaWriteHandler.UPDATE_RUNTIME);
        }
        registration.registerReadOnlyAttribute(InterfaceDefinition.NAME, new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                PathAddress address = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));
                context.getResult().set(address.getLastElement().getValue());
                context.completeStep();
            }
        });
    }
}
