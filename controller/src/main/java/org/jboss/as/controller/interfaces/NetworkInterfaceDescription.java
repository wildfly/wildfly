/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.controller.interfaces;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;
import org.jboss.as.controller.descriptions.common.InterfaceDescription;
import org.jboss.as.controller.parsing.Element;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @author Emanuel Muckenhuber
 */
public class NetworkInterfaceDescription {

    private static final String RESOURCE_NAME = InterfaceDescription.class.getPackage().getName() + ".LocalDescriptions";

    private static final String[] ALTERNATIVES_ANY = new String[] { ModelDescriptionConstants.ANY_ADDRESS, ModelDescriptionConstants.ANY_IPV4_ADDRESS, ModelDescriptionConstants.ANY_IPV6_ADDRESS};

    private static final String[] OTHERS = new String[] { localName(Element.INET_ADDRESS), localName(Element.LINK_LOCAL_ADDRESS),
            localName(Element.LOOPBACK), localName(Element.LOOPBACK_ADDRESS), localName(Element.MULTICAST), localName(Element.NIC),
            localName(Element.NIC_MATCH), localName(Element.POINT_TO_POINT), localName(Element.PUBLIC_ADDRESS), localName(Element.SITE_LOCAL_ADDRESS),
            localName(Element.SUBNET_MATCH), localName(Element.UP), localName(Element.VIRTUAL),
            localName(Element.ANY), localName(Element.NOT)
    };

    static final AttributeDefinition ANY_ADDRESS = SimpleAttributeDefinition.Builder.create(ModelDescriptionConstants.ANY_ADDRESS)
            .setType(ModelType.BOOLEAN).setAllowExpression(true).setAllowNull(true)
            .addAlternatives(OTHERS).addAlternatives(ModelDescriptionConstants.ANY_IPV4_ADDRESS, ModelDescriptionConstants.ANY_IPV6_ADDRESS)
            .create();
    static final AttributeDefinition ANY_IPV4_ADDRESS = SimpleAttributeDefinition.Builder.create(ModelDescriptionConstants.ANY_IPV4_ADDRESS)
            .setType(ModelType.BOOLEAN).setAllowExpression(true).setAllowNull(true)
            .addAlternatives(OTHERS).addAlternatives(ModelDescriptionConstants.ANY_ADDRESS, ModelDescriptionConstants.ANY_IPV6_ADDRESS)
            .create();
    static final AttributeDefinition ANY_IPV6_ADDRESS = SimpleAttributeDefinition.Builder.create(ModelDescriptionConstants.ANY_IPV6_ADDRESS)
            .setType(ModelType.BOOLEAN).setAllowExpression(true).setAllowNull(true)
            .addAlternatives(OTHERS).addAlternatives(ModelDescriptionConstants.ANY_ADDRESS, ModelDescriptionConstants.ANY_IPV4_ADDRESS)
            .create();
    static final AttributeDefinition INET_ADDRESS = SimpleAttributeDefinition.Builder.create(ModelDescriptionConstants.INET_ADDRESS)
            .setType(ModelType.STRING).setAllowExpression(true).setAllowNull(true).addAlternatives(ALTERNATIVES_ANY)
            .create();
    static final AttributeDefinition LINK_LOCAL_ADDRESS = SimpleAttributeDefinition.Builder.create(localName(Element.LINK_LOCAL_ADDRESS))
            .setType(ModelType.BOOLEAN).setAllowExpression(true).setAllowNull(true).addAlternatives(ALTERNATIVES_ANY)
            .create();
    static final AttributeDefinition LOOPBACK = SimpleAttributeDefinition.Builder.create(localName(Element.LOOPBACK))
            .setType(ModelType.BOOLEAN).setAllowExpression(true).setAllowNull(true).addAlternatives(ALTERNATIVES_ANY)
            .create();
    static final AttributeDefinition LOOPBACK_ADDRESS = SimpleAttributeDefinition.Builder.create(localName(Element.LOOPBACK_ADDRESS))
            .setType(ModelType.STRING).setAllowExpression(true).setAllowNull(true).addAlternatives(ALTERNATIVES_ANY)
            .create();
    static final AttributeDefinition NIC = SimpleAttributeDefinition.Builder.create(localName(Element.NIC))
            .setType(ModelType.STRING).setAllowExpression(true).setAllowNull(true).addAlternatives(ALTERNATIVES_ANY)
            .create();
    static final AttributeDefinition NIC_MATCH = SimpleAttributeDefinition.Builder.create(localName(Element.NIC_MATCH))
            .setType(ModelType.STRING).setAllowExpression(true).setAllowNull(true).addAlternatives(ALTERNATIVES_ANY)
            .create();
    static final AttributeDefinition MULTICAST = SimpleAttributeDefinition.Builder.create(localName(Element.MULTICAST))
            .setType(ModelType.BOOLEAN).setAllowExpression(true).setAllowNull(true).addAlternatives(ALTERNATIVES_ANY)
            .create();
    static final AttributeDefinition POINT_TO_POINT = SimpleAttributeDefinition.Builder.create(localName(Element.POINT_TO_POINT))
            .setType(ModelType.BOOLEAN).setAllowExpression(true).setAllowNull(true).addAlternatives(ALTERNATIVES_ANY)
            .create();
    static final AttributeDefinition PUBLIC_ADDRESS = SimpleAttributeDefinition.Builder.create(localName(Element.PUBLIC_ADDRESS))
            .setType(ModelType.BOOLEAN).setAllowExpression(true).setAllowNull(true).addAlternatives(ALTERNATIVES_ANY)
            .create();
    static final AttributeDefinition SITE_LOCAL_ADDRESS = SimpleAttributeDefinition.Builder.create(localName(Element.SITE_LOCAL_ADDRESS))
            .setType(ModelType.BOOLEAN).setAllowExpression(true).setAllowNull(true).addAlternatives(ALTERNATIVES_ANY)
            .create();
    static final AttributeDefinition SUBNET_MATCH = SimpleAttributeDefinition.Builder.create(localName(Element.SUBNET_MATCH))
            .setType(ModelType.STRING).setAllowExpression(true).setAllowNull(true).addAlternatives(ALTERNATIVES_ANY)
            .create();
    static final AttributeDefinition UP = SimpleAttributeDefinition.Builder.create(localName(Element.UP))
            .setType(ModelType.BOOLEAN).setAllowExpression(true).setAllowNull(true).addAlternatives(ALTERNATIVES_ANY)
            .create();
    static final AttributeDefinition VIRTUAL = SimpleAttributeDefinition.Builder.create(localName(Element.VIRTUAL))
            .setType(ModelType.BOOLEAN).setAllowExpression(true).setAllowNull(true).addAlternatives(ALTERNATIVES_ANY)
            .create();

    static final AttributeDefinition[] ROOT_ATTRIBUTES = new AttributeDefinition[] {

            ANY_ADDRESS, ANY_IPV4_ADDRESS, ANY_IPV6_ADDRESS, INET_ADDRESS, LINK_LOCAL_ADDRESS,
            LOOPBACK, LOOPBACK_ADDRESS, MULTICAST, NIC, NIC_MATCH, POINT_TO_POINT, PUBLIC_ADDRESS,
            SITE_LOCAL_ADDRESS, SUBNET_MATCH, UP, VIRTUAL

    };

    static final AttributeDefinition[] NESTED_ATTRIBUTES = new AttributeDefinition[] {
            INET_ADDRESS, LINK_LOCAL_ADDRESS, LOOPBACK, LOOPBACK_ADDRESS, MULTICAST, NIC,
            NIC_MATCH, POINT_TO_POINT, PUBLIC_ADDRESS, SITE_LOCAL_ADDRESS, SUBNET_MATCH, UP, VIRTUAL
    };


    static ListAttributeDefinition createList(final AttributeDefinition def) {
        final ListAttributeDefinition list = new ListAttributeDefinition(def.getName(), true, def.getValidator()) {
            @Override
            protected void addValueTypeDescription(final ModelNode node, final ResourceBundle bundle) {
                node.get(ModelDescriptionConstants.VALUE_TYPE).set(def.getType());
                final String[] alternatives = def.getAlternatives();
                if (alternatives != null) {
                    for(final String alternative : alternatives) {
                        node.get(ModelDescriptionConstants.ALTERNATIVES).add(alternative);
                    }
                }
            }
            @Override
            public void marshallAsElement(final ModelNode resourceModel, final XMLStreamWriter writer) throws XMLStreamException {
                throw new RuntimeException();
            }
        };
        return list;
    }

    static void addNested(final ModelNode valueType, final ResourceBundle bundle) {
        for(final AttributeDefinition def : NESTED_ATTRIBUTES) {

            final AttributeDefinition current;
            if(def.getType() == ModelType.STRING) {
                current = createList(def);
            } else {
                current = def;
            }
            final ModelNode m = current.createTypeDescription(bundle, "interface");
            m.remove(ModelDescriptionConstants.ALTERNATIVES);
            valueType.get(current.getName()).set(m);

        }
    }

    public static void main(String[] args) {

        final ResourceBundle bundle = ResourceBundle.getBundle(RESOURCE_NAME, Locale.getDefault());
        final ModelNode node = new ModelNode();

        for(final AttributeDefinition def : ROOT_ATTRIBUTES) {
            def.addResourceAttributeDescription(bundle, "interface", node);
        }

        node.get(ATTRIBUTES, localName(Element.ANY), TYPE).set(ModelType.OBJECT);
        node.get(ATTRIBUTES, localName(Element.ANY), DESCRIPTION);
        alternatives(node.get(ATTRIBUTES, localName(Element.ANY)), ALTERNATIVES_ANY);
        final ModelNode ANY = node.get(ATTRIBUTES, localName(Element.ANY), VALUE_TYPE);
        addNested(ANY, bundle);

        node.get(ATTRIBUTES, localName(Element.NOT), TYPE).set(ModelType.OBJECT);
        node.get(ATTRIBUTES, localName(Element.NOT), DESCRIPTION);
        alternatives(node.get(ATTRIBUTES, localName(Element.NOT)), ALTERNATIVES_ANY);
        final ModelNode NOT = node.get(ATTRIBUTES, localName(Element.NOT), VALUE_TYPE);
        addNested(NOT, bundle);

        System.out.println(node);

    }

    static void alternatives(final ModelNode node, final String[] alternatives) {
        if (alternatives != null) {
            for(final String alternative : alternatives) {
                node.get(ModelDescriptionConstants.ALTERNATIVES).add(alternative);
            }
        }
    }

    static String localName(final Element element) {
        return element.getLocalName();
    }

    // static final AttributeDefinition ANY_ADDRESS = new SimpleAttributeDefinition(ModelDescriptionConstants.ANY_ADDRESS, null, ModelType.BOOLEAN, true, copyAndAdd(OTHERS, ModelDescriptionConstants.ANY_IPV4_ADDRESS, ModelDescriptionConstants.ANY_IPV6_ADDRESS));
    // static final AttributeDefinition ANY_IPV4_ADDRESS = new SimpleAttributeDefinition(ModelDescriptionConstants.ANY_IPV4_ADDRESS, null, ModelType.BOOLEAN, true, copyAndAdd(OTHERS, ModelDescriptionConstants.ANY_ADDRESS, ModelDescriptionConstants.ANY_IPV6_ADDRESS));
    // static final AttributeDefinition ANY_IPV6_ADDRESS = new SimpleAttributeDefinition(ModelDescriptionConstants.ANY_IPV6_ADDRESS, null, ModelType.BOOLEAN, true, copyAndAdd(OTHERS, ModelDescriptionConstants.ANY_ADDRESS, ModelDescriptionConstants.ANY_IPV4_ADDRESS));
    //    static final AttributeDefinition INET_ADDRESS = new SimpleAttributeDefinition(ModelDescriptionConstants.INET_ADDRESS, null, ModelType.STRING, true, ALTERNATIVES_ANY);
//    static final AttributeDefinition LINK_LOCAL_ADDRESS = new SimpleAttributeDefinition(localName(Element.LINK_LOCAL_ADDRESS), null, ModelType.BOOLEAN, true, ALTERNATIVES_ANY);
//    static final AttributeDefinition LOOPBACK = new SimpleAttributeDefinition(localName(Element.LOOPBACK), null, ModelType.BOOLEAN, true, ALTERNATIVES_ANY);
//    static final AttributeDefinition LOOPBACK_ADDRESS = new SimpleAttributeDefinition(localName(Element.LOOPBACK_ADDRESS), null, ModelType.STRING, true, ALTERNATIVES_ANY);
//    static final AttributeDefinition NIC = new SimpleAttributeDefinition(localName(Element.NIC), null, ModelType.STRING, true, ALTERNATIVES_ANY);
//    static final AttributeDefinition NIC_MATCH = new SimpleAttributeDefinition(localName(Element.NIC_MATCH), null, ModelType.STRING, true, ALTERNATIVES_ANY);
//    static final AttributeDefinition MULTICAST = new SimpleAttributeDefinition(localName(Element.MULTICAST), null, ModelType.BOOLEAN, true, ALTERNATIVES_ANY);
//    static final AttributeDefinition POINT_TO_POINT = new SimpleAttributeDefinition(localName(Element.POINT_TO_POINT), null, ModelType.BOOLEAN, true, ALTERNATIVES_ANY);
//    static final AttributeDefinition PUBLIC_ADDRESS = new SimpleAttributeDefinition(localName(Element.PUBLIC_ADDRESS), null, ModelType.BOOLEAN, true, ALTERNATIVES_ANY);
//    static final AttributeDefinition SITE_LOCAL_ADDRESS = new SimpleAttributeDefinition(localName(Element.SITE_LOCAL_ADDRESS), null, ModelType.BOOLEAN, true, ALTERNATIVES_ANY);
//    static final AttributeDefinition SUBNET_MATCH = new SimpleAttributeDefinition(localName(Element.SUBNET_MATCH), null, ModelType.STRING, true, ALTERNATIVES_ANY);
//    static final AttributeDefinition UP = new SimpleAttributeDefinition(localName(Element.UP), null, ModelType.BOOLEAN, true, ALTERNATIVES_ANY);
//    static final AttributeDefinition VIRTUAL = new SimpleAttributeDefinition(localName(Element.VIRTUAL), null, ModelType.BOOLEAN, true, ALTERNATIVES_ANY);

}
