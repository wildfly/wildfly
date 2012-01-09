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

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;

import static org.jboss.as.controller.ControllerLogger.SERVER_LOGGER;
import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ANY_ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ANY_IPV4_ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ANY_IPV6_ADDRESS;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.jboss.as.controller.parsing.Element;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 * Utility class to create a interface criteria based on a {@link ModelNode} description
 *
 * @author Brian Stansberry
 * @author Emanuel Muckenhuber
 */
public final class ParsedInterfaceCriteria {

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

    public static ParsedInterfaceCriteria parse(final ModelNode criteria) {
        return parse(criteria, true);
    }

    public static ParsedInterfaceCriteria parse(final ModelNode model, boolean specified) {
        if (model.getType() != ModelType.OBJECT) {
            return new ParsedInterfaceCriteria(MESSAGES.illegalInterfaceCriteria(model.getType(), ModelType.OBJECT));
        }
        // Remove operation params
        final ModelNode subModel = model.clone();
        subModel.remove(ModelDescriptionConstants.OP);
        subModel.remove(ModelDescriptionConstants.OP_ADDR);
        final ParsedInterfaceCriteria parsed;
        if(subModel.hasDefined(ANY_ADDRESS) && subModel.get(ANY_ADDRESS).asBoolean(false)) {
            parsed = ParsedInterfaceCriteria.ANY;
        } else if(subModel.hasDefined(ANY_IPV4_ADDRESS) && subModel.get(ANY_IPV4_ADDRESS).asBoolean(false)) {
            parsed = ParsedInterfaceCriteria.V4;
        } else if(subModel.hasDefined(ANY_IPV6_ADDRESS) && subModel.get(ANY_IPV6_ADDRESS).asBoolean(false)) {
            parsed = ParsedInterfaceCriteria.V6;
        } else {
            try {
                final List<Property> nodes = subModel.asPropertyList();
                final Set<InterfaceCriteria> criteriaSet = new HashSet<InterfaceCriteria>();
                for (final Property property : nodes) {
                    final InterfaceCriteria criterion = parseCriteria(property, false);
                    if(criterion == null) {
                        // Ignore empty criteria
                        continue;
                    } else if (criterion instanceof WildcardInetAddressInterfaceCriteria) {
                        // AS7-1668: stop processing and just return the any binding.
                        if (nodes.size() > 1) {
                            SERVER_LOGGER.wildcardAddressDetected();
                        }
                        WildcardInetAddressInterfaceCriteria wc = (WildcardInetAddressInterfaceCriteria) criterion;
                        switch(wc.getVersion()) {
                            case V4: return ParsedInterfaceCriteria.V4;
                            case V6: return ParsedInterfaceCriteria.V6;
                            default: return ParsedInterfaceCriteria.ANY;
                        }
                    }
                    else {
                        criteriaSet.add(criterion);
                    }
                }
                parsed = new ParsedInterfaceCriteria(criteriaSet);
            } catch (ParsingException p) {
                return new ParsedInterfaceCriteria(p.msg);
            }
        }
        if (specified && parsed.getFailureMessage() == null && ! parsed.isAnyLocal() && ! parsed.isAnyLocalV4()
                && ! parsed.isAnyLocalV6() && parsed.getCriteria().size() == 0) {
            return new ParsedInterfaceCriteria(MESSAGES.noInterfaceCriteria());
        }
        return parsed;
    }

    private static InterfaceCriteria parseCriteria(final Property property, final boolean nested) {
        final Element element = Element.forName(property.getName());
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
            case INET_ADDRESS: {
                ModelNode value = property.getValue();
                value = parsePossibleExpression(value);
                checkStringType(value, element.getLocalName(), true);
                return createInetAddressCriteria(property.getValue());
            }
            case LOOPBACK_ADDRESS: {
                ModelNode value = property.getValue();
                value = parsePossibleExpression(value);
                checkStringType(value, element.getLocalName(), true);
                return new LoopbackAddressInterfaceCriteria(property.getValue());
            }
            case NIC: {
                checkStringType(property.getValue(), element.getLocalName());
                return new NicInterfaceCriteria(property.getValue().asString());
            }
            case NIC_MATCH: {
                checkStringType(property.getValue(), element.getLocalName());
                return createNicMatchCriteria(property.getValue());
            }
            case SUBNET_MATCH: {
                return createSubnetMatchCriteria(property.getValue());
            }
            case ANY:
            case NOT:
                if(nested) {
                    throw new ParsingException(MESSAGES.nestedElementNotAllowed(element));
                }
                return parseNested(property.getValue(), element == Element.ANY);
            default:
                throw new ParsingException(MESSAGES.unknownCriteriaInterfaceType(property.getName()));
        }
    }

    private static InterfaceCriteria parseNested(final ModelNode subModel, final boolean any) {
        if(!subModel.isDefined() || subModel.asInt() == 0) {
            return null;
        }
        final Set<InterfaceCriteria> criteriaSet = new HashSet<InterfaceCriteria>();
        for(final Property nestedProperty :  subModel.asPropertyList()) {
            final Element element = Element.forName(nestedProperty.getName());
            switch (element) {
                case INET_ADDRESS:
                case NIC :
                case NIC_MATCH:
                case SUBNET_MATCH: {
                    if (nestedProperty.getValue().getType() == ModelType.LIST) {
                        for (ModelNode item : nestedProperty.getValue().asList()) {
                            Property prop = new Property(nestedProperty.getName(), item);
                            InterfaceCriteria itemCriteria = parseCriteria(prop, true);
                            if(itemCriteria != null) {
                                criteriaSet.add(itemCriteria);
                            }
                        }
                        break;
                    } // else drop down into default: block
                }
                default: {
                    final InterfaceCriteria criteria = parseCriteria(nestedProperty, true);
                    if(criteria != null) {
                        criteriaSet.add(criteria);
                    }
                }
            }
        }
        if(criteriaSet.isEmpty()) {
            return null;
        }
        return any ? new AnyInterfaceCriteria(criteriaSet) : new NotInterfaceCriteria(criteriaSet);
    }

    private static InterfaceCriteria createInetAddressCriteria(final ModelNode model) throws ParsingException {
        try {
            String rawAddress = model.resolve().asString();
            InetAddress address = InetAddress.getByName(rawAddress);
            if (address.isAnyLocalAddress()) {
                // they've entered a wildcard address
                return new WildcardInetAddressInterfaceCriteria(address);
            } else {
                return new InetAddressMatchInterfaceCriteria(model);
            }
        } catch (UnknownHostException e) {
            throw new ParsingException(MESSAGES.invalidAddress(model.asString(),
                    e.getLocalizedMessage()));
        }
    }

    private static InterfaceCriteria createNicMatchCriteria(final ModelNode model) throws ParsingException {
        try {
            Pattern pattern = Pattern.compile(model.asString());
            return new NicMatchInterfaceCriteria(pattern);
        } catch (PatternSyntaxException e) {
            throw new ParsingException(MESSAGES.invalidInterfaceCriteriaPattern(model.asString(),
                    Element.NIC_MATCH.getLocalName()));
        }
    }

    private static InterfaceCriteria createSubnetMatchCriteria(final ModelNode model) throws ParsingException {
        String value;
        String[] split = null;
        try {
            value = model.asString();
            split = value.split("/");
            if (split.length != 2) {
                throw new ParsingException(MESSAGES.invalidAddressMaskValue(value));
            }
            // todo - possible DNS hit here
            final InetAddress addr = InetAddress.getByName(split[0]);
            // Validate both parts of the split
            final byte[] net = addr.getAddress();
            final int mask = Integer.parseInt(split[1]);
            return new SubnetMatchInterfaceCriteria(net, mask);
        } catch (final ParsingException e) {
            throw e;
        } catch (final NumberFormatException e) {
            throw new ParsingException(MESSAGES.invalidAddressMask(split[1], e.getLocalizedMessage()));
        } catch (final UnknownHostException e) {
            throw new ParsingException(MESSAGES.invalidAddressValue(split[0], e.getLocalizedMessage()));
        }
    }

    private static void checkStringType(ModelNode node, String id) {
        checkStringType(node, id, false);
    }

    private static void checkStringType(ModelNode node, String id, boolean allowExpressions) {
        if (node.getType() != ModelType.STRING && (!allowExpressions || node.getType() != ModelType.EXPRESSION)) {
            throw new ParsingException(MESSAGES.illegalValueForInterfaceCriteria(node.getType(), id, ModelType.STRING));
        }
    }
    private static ModelNode parsePossibleExpression(final ModelNode node) {
        return (node.getType() == ModelType.STRING) ? ParseUtils.parsePossibleExpression(node.asString()) : node;
    }

    private static class ParsingException extends RuntimeException {
        private static final long serialVersionUID = -5627251228393035383L;

        private final String msg;

        private ParsingException(String msg) {
            this.msg = msg;
        }
    }
}
