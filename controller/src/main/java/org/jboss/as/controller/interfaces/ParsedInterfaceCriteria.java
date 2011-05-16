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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ANY_ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ANY_IPV4_ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ANY_IPV6_ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NOT;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.jboss.as.controller.parsing.Element;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 * Utility class to create a interface criteria based on a {@link ModelNode} description
 *
 * @author Brian Stansberry
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

    public static ParsedInterfaceCriteria parse(final ModelNode criteria, boolean specified) {
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

    private static InterfaceCriteria parseStringCriteria(ModelNode node) {
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

    private static InterfaceCriteria parsePropertyCriteria(ModelNode node) {
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
                checkStringType(prop.getValue(), element.getLocalName(), true);
                return new InetAddressMatchInterfaceCriteria(prop.getValue());
            }
            case LOOPBACK_ADDRESS: {
                checkStringType(prop.getValue(), element.getLocalName(), true);
                return new LoopbackAddressInterfaceCriteria(prop.getValue());
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
                String value;
                String[] split = null;
                try {
                    value = prop.getValue().asString();
                    split = value.split("/");
                    if (split.length != 2) {
                        throw new ParsingException(String.format("Invalid 'value' %s -- must be of the form address/mask", value));
                    }
                    // todo - possible DNS hit here
                    final InetAddress addr = InetAddress.getByName(split[0]);
                    // Validate both parts of the split
                    final byte[] net = addr.getAddress();
                    final int mask = Integer.parseInt(split[1]);
                    return new SubnetMatchInterfaceCriteria(net, mask);
                }
                catch (final ParsingException e) {
                    throw e;
                }
                catch (final NumberFormatException e) {
                    throw new ParsingException(String.format("Invalid mask %s (%s)", split[0], e.getLocalizedMessage()));
                }
                catch (final UnknownHostException e) {
                    throw new ParsingException(String.format("Invalid address %s (%s)", split[1], e.getLocalizedMessage()));
                }
            }
            default:
                throw new ParsingException("Unknown complex interface criteria type " + node.asString());
        }
    }

    private static InterfaceCriteria parseCompoundCriteria(ModelNode value, boolean any) {
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

    private static void checkStringType(ModelNode node, String id) {
        checkStringType(node, id, false);
    }

    private static void checkStringType(ModelNode node, String id, boolean allowExpressions) {
        if (node.getType() != ModelType.STRING  && (!allowExpressions || node.getType() != ModelType.EXPRESSION)) {
            throw new ParsingException(String.format("Illegal value %s for interface criteria %; must be %s", node.getType(), id, ModelType.STRING));
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
