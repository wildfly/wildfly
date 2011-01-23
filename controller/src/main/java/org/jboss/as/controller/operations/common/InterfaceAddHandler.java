/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.controller.operations.common;


import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ANY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ANY_ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ANY_IPV4_ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ANY_IPV6_ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CRITERIA;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NOT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.InterfaceDescription;
import org.jboss.as.controller.interfaces.AnyInterfaceCriteria;
import org.jboss.as.controller.interfaces.InetAddressMatchInterfaceCriteria;
import org.jboss.as.controller.interfaces.InterfaceCriteria;
import org.jboss.as.controller.interfaces.LinkLocalInterfaceCriteria;
import org.jboss.as.controller.interfaces.LoopbackInterfaceCriteria;
import org.jboss.as.controller.interfaces.NicInterfaceCriteria;
import org.jboss.as.controller.interfaces.NicMatchInterfaceCriteria;
import org.jboss.as.controller.interfaces.NotInterfaceCriteria;
import org.jboss.as.controller.interfaces.PointToPointInterfaceCriteria;
import org.jboss.as.controller.interfaces.PublicAddressInterfaceCriteria;
import org.jboss.as.controller.interfaces.SiteLocalInterfaceCriteria;
import org.jboss.as.controller.interfaces.SubnetMatchInterfaceCriteria;
import org.jboss.as.controller.interfaces.SupportsMulticastInterfaceCriteria;
import org.jboss.as.controller.interfaces.UpInterfaceCriteria;
import org.jboss.as.controller.interfaces.VirtualInterfaceCriteria;
import org.jboss.as.controller.parsing.Element;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 * Handler for the interface resource add operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class InterfaceAddHandler implements ModelAddOperationHandler, DescriptionProvider {

    public static final String OPERATION_NAME = ADD;

    public static ModelNode getAddInterfaceOperation(ModelNode address, ModelNode criteria) {
        ModelNode op = Util.getEmptyOperation(ADD, address);
        op.get(CRITERIA).set(criteria);
        return op;
    }

    public static final InterfaceAddHandler NAMED_INSTANCE = new InterfaceAddHandler(false);

    public static final InterfaceAddHandler SPECIFIED_INSTANCE = new InterfaceAddHandler(true);

    private final boolean specified;

    /**
     * Create the InterfaceAddHandler
     */
    protected InterfaceAddHandler(boolean specified) {
        this.specified = specified;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cancellable execute(NewOperationContext context, ModelNode operation, ResultHandler resultHandler) {
        try {
            PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
            String name = address.getLastElement().getValue();
            ModelNode model = context.getSubModel();
            model.get(NAME).set(name);

            ModelNode criteriaNode = operation.get(CRITERIA);
            ParsedInterfaceCriteria parsed = parseCriteria(criteriaNode.clone());
            if (parsed.getFailureMessage() == null) {
                model.get(CRITERIA).set(criteriaNode);
                ModelNode compensating = Util.getResourceRemoveOperation(operation.get(OP_ADDR));
                installInterface(name, parsed, context, resultHandler, compensating);
            }
            else {
                resultHandler.handleFailed(new ModelNode().set(parsed.getFailureMessage()));
            }
        }
        catch (Exception e) {
            resultHandler.handleFailed(new ModelNode().set(e.getLocalizedMessage()));
        }
        return Cancellable.NULL;
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return specified ? InterfaceDescription.getSpecifiedInterfaceAddOperation(locale) : InterfaceDescription.getNamedInterfaceAddOperation(locale);
    }

    protected void installInterface(String name, ParsedInterfaceCriteria criteria, NewOperationContext context, ResultHandler resultHandler, ModelNode compensatingOp) {
        resultHandler.handleResultComplete(compensatingOp);
    }

    private ParsedInterfaceCriteria parseCriteria(ModelNode criteria) {
        ParsedInterfaceCriteria parsed = ParsedInterfaceCriteria.EMPTY;
        if (criteria.isDefined()) {
            if (criteria.getType() == ModelType.STRING) {
                String crit = criteria.asString();
                if (ANY_ADDRESS.equals(crit)) {
                    parsed = ParsedInterfaceCriteria.ANY;
                }
                else if (ANY_IPV4_ADDRESS.equals(crit)) {
                    parsed = ParsedInterfaceCriteria.V4;
                }
                else if (ANY_IPV6_ADDRESS.equals(crit)) {
                    parsed = ParsedInterfaceCriteria.V6;
                }
                else {
                    return new ParsedInterfaceCriteria(String.format("Illegal interface criteria %s; must be %s, %s or %s or a list of criteria elements", crit, ANY_ADDRESS, ANY_IPV4_ADDRESS, ANY_IPV6_ADDRESS));
                }
            }
            else if (criteria.getType() == ModelType.LIST) {
                try {
                    Set<InterfaceCriteria> criteriaSet = new HashSet<InterfaceCriteria>();
                    for (ModelNode node : criteria.asList()) {
                        if (node.getType() == ModelType.STRING) {
                            criteriaSet.add(parseStringCriteria(node));
                        }
                        else if (node.getType() == ModelType.PROPERTY) {
                            criteriaSet.add(parsePropertyCriteria(node));
                        }
                        else {
                            return new ParsedInterfaceCriteria(String.format("Illegal interface criteria list element type %s; must be %s or %s", node.getType(), ModelType.STRING, ModelType.PROPERTY));
                        }
                    }
                    return new ParsedInterfaceCriteria(criteriaSet);
                }
                catch (ParsingException p) {
                    return new ParsedInterfaceCriteria(p.msg);
                }
            }
            else {
                return new ParsedInterfaceCriteria(String.format("Illegal interface criteria type %s; must be %s or %s", criteria.getType(), ModelType.STRING, ModelType.LIST));
            }
        }

        if (specified && parsed.getFailureMessage() == null && !parsed.isAnyLocal()
                && !parsed.isAnyLocalV4() && !parsed.isAnyLocalV6() && parsed.getCriteria().size() ==  0) {
            parsed = new ParsedInterfaceCriteria("No interface criteria was provided");
        }
        return parsed;
    }

    private InterfaceCriteria parseStringCriteria(ModelNode node) {
        final Element element = Element.forName(node.asString());
        switch (element) {
            case LINK_LOCAL_ADDRESS:
                return LinkLocalInterfaceCriteria.INSTANCE;
            case LOOPBACK:
                return LoopbackInterfaceCriteria.INSTANCE;
            case MULTICAST:
                return SupportsMulticastInterfaceCriteria.INSTANCE;
            case POINT_TO_POINT:
                return PointToPointInterfaceCriteria.INSTANCE;
            case PUBLIC_ADDRESS:
                return PublicAddressInterfaceCriteria.INSTANCE;
            case SITE_LOCAL_ADDRESS:
                return SiteLocalInterfaceCriteria.INSTANCE;
            case UP:
                return UpInterfaceCriteria.INSTANCE;
            case VIRTUAL:
                return VirtualInterfaceCriteria.INSTANCE;
            default:
                throw new ParsingException("Unknown simple interface criteria type " + node.asString());
        }
    }

    private InterfaceCriteria parsePropertyCriteria(ModelNode node) {
        Property prop = node.asProperty();
        String propName = prop.getName();
        final Element element = Element.forName(propName);
        switch (element) {
            case ANY: {
                return parseCompoundCriteria(prop.getValue(), true);
            }
            case NOT: {
                return parseCompoundCriteria(prop.getValue(), true);
            }
            case INET_ADDRESS: {
                checkStringType(prop.getValue(), element.getLocalName());
                return new InetAddressMatchInterfaceCriteria(prop.getValue().asString());
            }
            case NIC: {
                checkStringType(prop.getValue(), element.getLocalName());
                return new NicInterfaceCriteria(prop.getValue().asString());
            }
            case NIC_MATCH: {
                checkStringType(prop.getValue(), element.getLocalName());
                try {
                    Pattern pattern = Pattern.compile(prop.getValue().asString());
                    return new NicMatchInterfaceCriteria(pattern);
                } catch (PatternSyntaxException e) {
                    throw new ParsingException(String.format("Invalid pattern %s for interface criteria %s", prop.getValue().asString(), element.getLocalName()));
                }
            }
            case SUBNET_MATCH: {
                String key = "network";
                ModelType type = ModelType.BYTES;
                try {
                    ModelNode value = prop.getValue();
                    byte[] net = value.require(key).asBytes();
                    key = "mask";
                    type = ModelType.INT;
                    int mask = value.require(key).asInt();
                    return new SubnetMatchInterfaceCriteria(net, mask);
                }
                catch (Exception e) {
                    throw new ParsingException(String.format("Interface criteria %s must have field %s of type %s", element.getLocalName(), key, type));
                }
            }
            default:
                throw new ParsingException("Unknown complex interface criteria type " + node.asString());
        }
    }

    private InterfaceCriteria parseCompoundCriteria(ModelNode value, boolean any) {
        if (value.getType() == ModelType.LIST) {
            Set<InterfaceCriteria> nested = new HashSet<InterfaceCriteria>();
            for (ModelNode element : value.asList()) {
                if (element.getType() == ModelType.STRING) {
                    nested.add(parseStringCriteria(element));
                }
                else if (element.getType() == ModelType.PROPERTY) {
                    nested.add(parsePropertyCriteria(element));
                }
                else {
                    throw new ParsingException(String.format("Illegal interface criteria list element type %s; must be %s or %s", value.getType(), ModelType.STRING, ModelType.PROPERTY));
                }
            }
            return any ? new AnyInterfaceCriteria(nested) : new NotInterfaceCriteria(nested);
        }
        else  {
            throw new ParsingException(String.format("Illegal child type %s for criteria type %s; must be %s", value.getType(), any ? ANY : NOT, ModelType.LIST));
        }
    }

    private void checkStringType(ModelNode node, String id) {
        if (node.getType() != ModelType.STRING) {
            throw new ParsingException(String.format("Illegal value %s for interface criteria %; must be %s", node.getType(), id, ModelType.STRING));
        }

    }

    protected static class ParsedInterfaceCriteria {

        private static final ParsedInterfaceCriteria EMPTY = new ParsedInterfaceCriteria();
        private static final ParsedInterfaceCriteria ANY = new ParsedInterfaceCriteria(false, false, true);
        private static final ParsedInterfaceCriteria V4 = new ParsedInterfaceCriteria(true, false, false);
        private static final ParsedInterfaceCriteria V6 = new ParsedInterfaceCriteria(false, true, false);

        private final String failureMessage;
        private final boolean anyLocalV4;
        private final boolean anyLocalV6;
        private final boolean anyLocal;
        private final Set<InterfaceCriteria> criteria = new HashSet<InterfaceCriteria>();

        private ParsedInterfaceCriteria() {
            this.failureMessage = null;
            this.anyLocal = anyLocalV4 = anyLocalV6 = false;
        }

        private ParsedInterfaceCriteria(final String failureMessage) {
            this.failureMessage = failureMessage;
            this.anyLocal = anyLocalV4 = anyLocalV6 = false;
        }

        private ParsedInterfaceCriteria(final boolean anyLocalV4, final boolean anyLocalV6, final boolean anyLocal) {
            this.failureMessage = null;
            this.anyLocal = anyLocal;
            this.anyLocalV4 = anyLocalV4;
            this.anyLocalV6 = anyLocalV6;
        }

        private ParsedInterfaceCriteria(final Set<InterfaceCriteria> criteria) {
            this.failureMessage = null;
            this.anyLocal = anyLocalV4 = anyLocalV6 = false;
            this.criteria.addAll(criteria);
        }

        public String getFailureMessage() {
            return failureMessage;
        }

        public boolean isAnyLocalV4() {
            return anyLocalV4;
        }

        public boolean isAnyLocalV6() {
            return anyLocalV6;
        }

        public boolean isAnyLocal() {
            return anyLocal;
        }

        public Set<InterfaceCriteria> getCriteria() {
            return criteria;
        }
    }

    private static class ParsingException extends RuntimeException {
        private static final long serialVersionUID = -5627251228393035383L;

        private final String msg;

        private ParsingException(String msg) {
            this.msg = msg;
        }
    }
}
