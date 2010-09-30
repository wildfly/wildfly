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

import java.util.Locale;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractModelElement;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.jboss.xnio.OptionMap;
import org.jboss.xnio.Options;
import org.jboss.xnio.SaslQop;
import org.jboss.xnio.SaslStrength;
import org.jboss.xnio.Sequence;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class SaslElement extends AbstractModelElement<SaslElement> {

    private static final long serialVersionUID = -7152729794181116303L;

    private PolicyElement policy;
    private String[] includeMechanisms;
    private SaslQop[] qop;
    private SaslStrength[] strength;
    private Boolean reuseSession;
    private Boolean serverAuth;
    private Map<String, String> properties;

    public SaslElement() {
        //
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
        if (properties != null && ! properties.isEmpty()) {
            streamWriter.writeStartElement(Element.PROPERTIES.getLocalName());
            for (String key : properties.keySet()) {
                streamWriter.writeEmptyElement(Element.PROPERTY.getLocalName());
                streamWriter.writeAttribute(Attribute.NAME.getLocalName(), key);
                streamWriter.writeAttribute(Attribute.VALUE.getLocalName(), properties.get(key));
            }
            streamWriter.writeEndElement();
        }
        streamWriter.writeEndElement();
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }


    public PolicyElement getPolicy() {
        return policy;
    }

    void setPolicy(PolicyElement policy) {
        this.policy = policy;
    }

    public String[] getIncludeMechanisms() {
        return includeMechanisms;
    }

    void setIncludeMechanisms(String[] includeMechanisms) {
        this.includeMechanisms = includeMechanisms;
    }

    public SaslQop[] getQop() {
        return qop;
    }

    void setQop(SaslQop[] qop) {
        this.qop = qop;
    }

    public SaslStrength[] getStrength() {
        return strength;
    }

    void setStrength(SaslStrength[] strength) {
        this.strength = strength;
    }

    public Boolean getReuseSession() {
        return reuseSession;
    }

    void setReuseSession(Boolean reuseSession) {
        this.reuseSession = reuseSession;
    }

    public Boolean getServerAuth() {
        return serverAuth;
    }

    void setServerAuth(Boolean serverAuth) {
        this.serverAuth = serverAuth;
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
