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
import org.jboss.as.model.AbstractModelElement;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.jboss.xnio.OptionMap;
import org.jboss.xnio.Options;

import javax.xml.stream.XMLStreamException;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class PolicyElement extends AbstractModelElement<PolicyElement> {

    private static final long serialVersionUID = -7621176658685285003L;

    private Boolean forwardSecrecy;
    private Boolean noActive;
    private Boolean noAnonymous;
    private Boolean noDictionary;
    private Boolean noPlainText;
    private Boolean passCredentials;

    public PolicyElement() {
    }

    public PolicyElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        if (reader.getAttributeCount() > 0) {
            throw unexpectedAttribute(reader, 0);
        }
        // Handle nested elements.
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
                        case FORWARD_SECRECY: {
                            forwardSecrecy = Boolean.valueOf(readBooleanAttributeElement(reader, "value"));
                            break;
                        }
                        case NO_ACTIVE: {
                            noActive = Boolean.valueOf(readBooleanAttributeElement(reader, "value"));
                            break;
                        }
                        case NO_ANONYMOUS: {
                            noAnonymous = Boolean.valueOf(readBooleanAttributeElement(reader, "value"));
                            break;
                        }
                        case NO_DICTIONARY: {
                            noDictionary = Boolean.valueOf(readBooleanAttributeElement(reader, "value"));
                            break;
                        }
                        case NO_PLAINTEXT: {
                            noPlainText = Boolean.valueOf(readBooleanAttributeElement(reader, "value"));
                            break;
                        }
                        case PASS_CREDENTIALS: {
                            passCredentials = Boolean.valueOf(readBooleanAttributeElement(reader, "value"));
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

    public long elementHash() {
        long hash = 0L;
        if (forwardSecrecy != null) { hash = forwardSecrecy.hashCode() & 0xffffffffL; }
        if (noActive != null) { hash = (noActive.hashCode() & 0xffffffffL) << 1 ^ hash; }
        if (noAnonymous != null) { hash = (noAnonymous.hashCode() & 0xffffffffL) << 2 ^ hash; }
        if (noDictionary != null) { hash = (noDictionary.hashCode() & 0xffffffffL) << 3 ^ hash; }
        if (noPlainText != null) { hash = (noPlainText.hashCode() & 0xffffffffL) << 4 ^ hash; }
        if (passCredentials != null) { hash = (passCredentials.hashCode() & 0xffffffffL) << 5 ^ hash; }
        return hash;
    }

    protected Class<PolicyElement> getElementClass() {
        return PolicyElement.class;
    }

    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        if (forwardSecrecy != null) {
            streamWriter.writeEmptyElement("forward-secrecy");
            streamWriter.writeAttribute("value", forwardSecrecy.toString());
        }
        if (noActive != null) {
            streamWriter.writeEmptyElement("no-active");
            streamWriter.writeAttribute("value", noActive.toString());
        }
        if (noAnonymous != null) {
            streamWriter.writeEmptyElement("no-anonymous");
            streamWriter.writeAttribute("value", noAnonymous.toString());
        }
        if (noDictionary != null) {
            streamWriter.writeEmptyElement("no-dictionary");
            streamWriter.writeAttribute("value", noDictionary.toString());
        }
        if (noPlainText != null) {
            streamWriter.writeEmptyElement("no-plain-text");
            streamWriter.writeAttribute("value", noPlainText.toString());
        }
        if (passCredentials != null) {
            streamWriter.writeEmptyElement("pass-credentials");
            streamWriter.writeAttribute("value", passCredentials.toString());
        }
        streamWriter.writeEndElement();
    }

    public void applyTo(OptionMap.Builder builder) {
        if (forwardSecrecy != null) {
            builder.set(Options.SASL_POLICY_FORWARD_SECRECY, forwardSecrecy);
        }
        if (noActive != null) {
            builder.set(Options.SASL_POLICY_NOACTIVE, noActive);
        }
        if (noAnonymous != null) {
            builder.set(Options.SASL_POLICY_NOANONYMOUS, noAnonymous);
        }
        if (noDictionary != null) {
            builder.set(Options.SASL_POLICY_NODICTIONARY, noDictionary);
        }
        if (noPlainText != null) {
            builder.set(Options.SASL_POLICY_NOPLAINTEXT, noPlainText);
        }
        if (passCredentials != null) {
            builder.set(Options.SASL_POLICY_PASS_CREDENTIALS, passCredentials);
        }
    }
}
