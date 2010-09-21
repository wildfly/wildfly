/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.remoting;

import java.util.EnumSet;
import java.util.Locale;
import org.jboss.as.model.AbstractModelElement;
import org.jboss.as.model.PropertiesElement;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.jboss.xnio.OptionMap;
import org.jboss.xnio.Options;
import org.jboss.xnio.SaslQop;
import org.jboss.xnio.SaslStrength;
import org.jboss.xnio.Sequence;

import javax.xml.stream.XMLStreamException;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class SaslElement extends AbstractModelElement<SaslElement> {

    private static final long serialVersionUID = -7152729794181116303L;

    private PropertiesElement properties;
    private PolicyElement policy;
    private String[] includeMechanisms;
    private SaslQop[] qop;
    private SaslStrength[] strength;
    private Boolean reuseSession;
    private Boolean serverAuth;

    public SaslElement() {
    }

    public SaslElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        // No attributes
        final int count = reader.getAttributeCount();
        if (count > 0) {
            throw unexpectedAttribute(reader, 0);
        }
        // Nested elements
        final EnumSet<Element> visited = EnumSet.noneOf(Element.class);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case REMOTING_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    if (visited.contains(element)) {
                        throw unexpectedElement(reader);
                    }
                    visited.add(element);
                    switch (element) {
                        case INCLUDE_MECHANISMS: {
                            includeMechanisms = readArrayAttributeElement(reader, "value", String.class);
                            break;
                        }
                        case POLICY: {
                            policy = new PolicyElement(reader);
                            break;
                        }
                        case PROPERTIES: {
                            properties = new PropertiesElement(reader);
                            break;
                        }
                        case QOP: {
                            qop = readArrayAttributeElement(reader, "value", SaslQop.class);
                            break;
                        }
                        case REUSE_SESSION: {
                            reuseSession = Boolean.valueOf(readBooleanAttributeElement(reader, "value"));
                            break;
                        }
                        case SERVER_AUTH: {
                            serverAuth = Boolean.valueOf(readBooleanAttributeElement(reader, "value"));
                            break;
                        }
                        case STRENGTH: {
                            strength = readArrayAttributeElement(reader, "value", SaslStrength.class);
                            break;
                        }
                        default: throw unexpectedElement(reader);
                    }
                    break;
                }
                default: throw unexpectedElement(reader);
            }
        }
    }

    /** {@inheritDoc} */
    public long elementHash() {
        long hash = 0L;
        if (properties != null) hash ^= properties.elementHash();
        if (policy != null) hash = Long.rotateLeft(hash, 1) ^ policy.elementHash();
        if (includeMechanisms != null) hash = calculateElementHashOf(includeMechanisms, hash);
        if (qop != null) hash = calculateElementHashOf(qop, hash);
        if (strength != null) hash = calculateElementHashOf(strength, hash);
        if (reuseSession != null) hash = Long.rotateLeft(hash, 1) ^ reuseSession.hashCode();
        if (serverAuth != null) hash = Long.rotateLeft(hash, 1) ^ serverAuth.hashCode();
        return hash;
    }

    /** {@inheritDoc} */
    protected Class<SaslElement> getElementClass() {
        return SaslElement.class;
    }

    /** {@inheritDoc} */
    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        if (includeMechanisms != null) {
            streamWriter.writeEmptyElement("include-mechanisms");
            streamWriter.writeAttribute("value", includeMechanisms);
        }
        if (qop != null) {
            streamWriter.writeEmptyElement("qop");
            streamWriter.writeAttribute("value", qop.toString().toLowerCase(Locale.ENGLISH));
        }
        if (strength != null) {
            streamWriter.writeEmptyElement("strength");
            streamWriter.writeAttribute("value", strength.toString().toLowerCase(Locale.ENGLISH));
        }
        if (reuseSession != null) {
            streamWriter.writeEmptyElement("reuse-session");
            streamWriter.writeAttribute("value", reuseSession.toString());
        }
        if (serverAuth != null) {
            streamWriter.writeEmptyElement("server-auth");
            streamWriter.writeAttribute("value", serverAuth.toString());
        }
        if (policy != null) {
            streamWriter.writeStartElement("policy");
            policy.writeContent(streamWriter);
        }
        if (properties != null) {
            streamWriter.writeStartElement("properties");
            properties.writeContent(streamWriter);
        }
        streamWriter.writeEndElement();
    }

    /**
     * Apply this configuration to an option map.
     *
     * @param builder the option map builder
     */
    public void applyTo(OptionMap.Builder builder) {
        // todo: properties element
        if (policy != null) {
            policy.applyTo(builder);
        }
        if (includeMechanisms != null) {
            builder.set(Options.SASL_MECHANISMS, Sequence.of(includeMechanisms));
        }
        if (qop != null) {
            builder.set(Options.SASL_QOP, Sequence.of(qop));
        }
        if (strength != null) {
            // todo - fix this in XNIO
//            builder.set(Options.SASL_STRENGTH, Sequence.of(strength));
        }
        if (reuseSession != null) {
            builder.set(Options.SASL_REUSE, reuseSession);
        }
        if (serverAuth != null) {
            builder.set(Options.SASL_SERVER_AUTH, serverAuth);
        }
    }
}
