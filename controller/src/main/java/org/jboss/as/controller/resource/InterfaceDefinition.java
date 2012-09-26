package org.jboss.as.controller.resource;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALTERNATIVES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceNameOperationStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.operations.common.InterfaceAddHandler;
import org.jboss.as.controller.operations.common.InterfaceCriteriaWriteHandler;
import org.jboss.as.controller.operations.common.InterfaceRemoveHandler;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.parsing.Element;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class InterfaceDefinition extends SimpleResourceDefinition {
    public static final String[] ALTERNATIVES_ANY = new String[]{ModelDescriptionConstants.ANY_ADDRESS, ModelDescriptionConstants.ANY_IPV4_ADDRESS, ModelDescriptionConstants.ANY_IPV6_ADDRESS};
    public static final String[] OTHERS = new String[]{localName(Element.INET_ADDRESS), localName(Element.LINK_LOCAL_ADDRESS),
            localName(Element.LOOPBACK), localName(Element.LOOPBACK_ADDRESS), localName(Element.MULTICAST), localName(Element.NIC),
            localName(Element.NIC_MATCH), localName(Element.POINT_TO_POINT), localName(Element.PUBLIC_ADDRESS), localName(Element.SITE_LOCAL_ADDRESS),
            localName(Element.SUBNET_MATCH), localName(Element.UP), localName(Element.VIRTUAL),
            localName(Element.ANY), localName(Element.NOT)
    };


    public static final AttributeDefinition NAME = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.NAME, ModelType.STRING)
            .setResourceOnly()
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

    public static final AttributeDefinition[] NESTED_ATTRIBUTES = new AttributeDefinition[]{
            INET_ADDRESS, LINK_LOCAL_ADDRESS, LOOPBACK, LOOPBACK_ADDRESS, MULTICAST, NIC,
            NIC_MATCH, POINT_TO_POINT, PUBLIC_ADDRESS, SITE_LOCAL_ADDRESS, SUBNET_MATCH, UP, VIRTUAL
    };

    public static final AttributeDefinition NOT = createNestedComplexType("not");
    public static final AttributeDefinition ANY = createNestedComplexType("any");

    /*public static final ObjectTypeAttributeDefinition NOT = new ObjectTypeAttributeDefinition.Builder("not", NESTED_ATTRIBUTES)
            .setAllowNull(true)
            .build();

    public static final ObjectTypeAttributeDefinition ANY = new ObjectTypeAttributeDefinition.Builder("any", NESTED_ATTRIBUTES)
            .setAllowNull(true)
            .build();*/


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

    public static final Set<AttributeDefinition> NESTED_LIST_ATTRIBUTES = new HashSet<AttributeDefinition>(
            Arrays.asList(INET_ADDRESS, NIC, NIC_MATCH, SUBNET_MATCH)
    );
    /**
     * The wildcard criteria attributes
     */
    /*public static final AttributeDefinition[] WILDCARD_ATTRIBUTES = new AttributeDefinition[]{ANY_ADDRESS, ANY_IPV4_ADDRESS, ANY_IPV6_ADDRESS};
    public static final AttributeDefinition[] SIMPLE_ATTRIBUTES = new AttributeDefinition[]{LINK_LOCAL_ADDRESS, LOOPBACK,
            MULTICAST, POINT_TO_POINT, PUBLIC_ADDRESS, SITE_LOCAL_ADDRESS, UP, VIRTUAL};*/

    private final boolean updateRuntime;

    public InterfaceDefinition(InterfaceAddHandler addHandler, InterfaceRemoveHandler removeHandler, boolean updateRuntime) {
        super(PathElement.pathElement(INTERFACE),
                ControllerResolver.getResolver(INTERFACE),
                addHandler,
                removeHandler);
        this.updateRuntime = updateRuntime;
    }

    public static String localName(final Element element) {
        return element.getLocalName();
    }

    /**
     * Test whether the operation has a defined criteria attribute.
     *
     * @param operation the operation
     * @return
     */
    public static boolean isOperationDefined(final ModelNode operation) {
        for(final AttributeDefinition def : ROOT_ATTRIBUTES) {
            if(operation.hasDefined(def.getName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void registerOperations(ManagementResourceRegistration interfaces) {
        super.registerOperations(interfaces);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        super.registerAttributes(registration);
        InterfaceCriteriaWriteHandler handler = updateRuntime ? InterfaceCriteriaWriteHandler.UPDATE_RUNTIME : InterfaceCriteriaWriteHandler.CONFIG_ONLY;
        for (final AttributeDefinition def : InterfaceDefinition.ROOT_ATTRIBUTES) {
            registration.registerReadWriteAttribute(def, null, handler);
        }
        registration.registerReadOnlyAttribute(InterfaceDefinition.NAME, ResourceNameOperationStepHandler.INSTANCE);
    }

    @Deprecated
    private static AttributeDefinition createNestedComplexType(final String name) {
        return new AttributeDefinition(name, name, null, ModelType.OBJECT, true, false, MeasurementUnit.NONE, createNestedParamValidator(), ALTERNATIVES_ANY, null, AttributeAccess.Flag.RESTART_ALL_SERVICES) {

            @Override
            public ModelNode addResourceAttributeDescription(final ResourceBundle bundle, final String prefix, final ModelNode resourceDescription) {
                final ModelNode result = super.addResourceAttributeDescription(bundle, prefix, resourceDescription);
                addNestedDescriptions(result, prefix, bundle);
                return result;
            }

            @Override
            public ModelNode addOperationParameterDescription(ResourceBundle bundle, String prefix, ModelNode operationDescription) {
                final ModelNode result = super.addOperationParameterDescription(bundle, prefix, operationDescription);
                addNestedDescriptions(result, prefix, bundle);
                return result;
            }

            @Override
            public ModelNode addResourceAttributeDescription(ModelNode resourceDescription, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
                ModelNode result = super.addResourceAttributeDescription(resourceDescription, resolver, locale, bundle);
                for (final AttributeDefinition def : NESTED_ATTRIBUTES) {
                    result.get(VALUE_TYPE, def.getName(), ModelDescriptionConstants.DESCRIPTION).set(resolver.getResourceAttributeDescription(def.getName(), locale, bundle));
                }
                return result;
            }

            @Override
            public ModelNode addOperationParameterDescription(ModelNode resourceDescription, String operationName, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
                ModelNode result = super.addOperationParameterDescription(resourceDescription, operationName, resolver, locale, bundle);
                for (final AttributeDefinition def : NESTED_ATTRIBUTES) {
                    result.get(VALUE_TYPE, def.getName(), ModelDescriptionConstants.DESCRIPTION).set(resolver.getOperationParameterDescription(operationName, def.getName(), locale, bundle));
                }
                return result;
            }

            void addNestedDescriptions(final ModelNode result, final String prefix, final ResourceBundle bundle) {
                for (final AttributeDefinition def : NESTED_ATTRIBUTES) {
                    final String bundleKey = prefix == null ? def.getName() : (prefix + "." + def.getName());
                    result.get(VALUE_TYPE, def.getName(), ModelDescriptionConstants.DESCRIPTION).set(bundle.getString(bundleKey));
                }
            }

            @Override
            public ModelNode getNoTextDescription(boolean forOperation) {
                final ModelNode model = super.getNoTextDescription(forOperation);
                final ModelNode valueType = model.get(VALUE_TYPE);
                for (final AttributeDefinition def : NESTED_ATTRIBUTES) {
                    final AttributeDefinition current;
                    if (NESTED_LIST_ATTRIBUTES.contains(def)) {
                        current = wrapAsList(def);
                    } else {
                        current = def;
                    }
                    final ModelNode m = current.getNoTextDescription(forOperation);
                    m.remove(ALTERNATIVES);
                    valueType.get(current.getName()).set(m);
                }
                return model;
            }
        };

    }

    /**
     * Wrap a simple attribute def as list.
     *
     * @param def the attribute definition
     * @return the list attribute def
     */
    @Deprecated
    private static ListAttributeDefinition wrapAsList(final AttributeDefinition def) {
        final ListAttributeDefinition list = new ListAttributeDefinition(def.getName(), true, def.getValidator()) {

            @Override
            public ModelNode getNoTextDescription(boolean forOperation) {
                final ModelNode model = super.getNoTextDescription(forOperation);
                setValueType(model);
                return model;
            }

            @Override
            protected void addValueTypeDescription(final ModelNode node, final ResourceBundle bundle) {
                setValueType(node);
            }

            @Override
            public void marshallAsElement(final ModelNode resourceModel, final boolean marshalDefault, final XMLStreamWriter writer) throws XMLStreamException {
                throw new RuntimeException();
            }

            @Override
            protected void addAttributeValueTypeDescription(ModelNode node, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
                setValueType(node);
            }

            @Override
            protected void addOperationParameterValueTypeDescription(ModelNode node, String operationName, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
                setValueType(node);
            }

            private void setValueType(ModelNode node) {
                node.get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.STRING);
            }
        };
        return list;
    }

    @Deprecated
    static ParameterValidator createNestedParamValidator() {
        return new ModelTypeValidator(ModelType.OBJECT, true, false, true) {
            @Override
            public void validateParameter(final String parameterName, final ModelNode value) throws OperationFailedException {
                super.validateParameter(parameterName, value);

                for (final AttributeDefinition def : NESTED_ATTRIBUTES) {
                    final String name = def.getName();
                    if (value.hasDefined(name)) {
                        final ModelNode v = value.get(name);
                        if (NESTED_LIST_ATTRIBUTES.contains(def)) {
                            if (ModelType.LIST != v.getType()) {
                                throw new OperationFailedException(new ModelNode().set(MESSAGES.invalidType(v.getType())));
                            }
                        } else {
                            def.getValidator().validateParameter(name, v);
                        }
                    }
                }
            }
        };
    }

}
