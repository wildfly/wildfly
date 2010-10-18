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

package org.jboss.as.web;

import java.util.Set;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractModelElement;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author Emanuel Muckenhuber
 */
public class WebVirtualHostElement extends AbstractModelElement<WebVirtualHostElement> {

    private static final long serialVersionUID = 5587346142770662451L;
    private final String name;
    private Set<String> aliases;
    private WebHostAccessLogElement accessLog;
    private WebHostRewriteElement rewrite;

    public WebVirtualHostElement(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Set<String> getAliases() {
        return aliases;
    }

    public void setAliases(Set<String> aliases) {
        this.aliases = aliases;
    }

    public WebHostAccessLogElement getAccessLog() {
        return accessLog;
    }

    public void setAccessLog(WebHostAccessLogElement accessLog) {
        this.accessLog = accessLog;
    }

    public WebHostRewriteElement getRewrite() {
        return rewrite;
    }

    public void setRewrite(WebHostRewriteElement rewrite) {
        this.rewrite = rewrite;
    }

    /** {@inheritDoc} */
    protected Class<WebVirtualHostElement> getElementClass() {
        return WebVirtualHostElement.class;
    }

    /** {@inheritDoc} */
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.NAME.getLocalName(), name);
        if(! aliases.isEmpty()) {
            for(final String alias : aliases) {
                streamWriter.writeEmptyElement(Element.ALIAS.getLocalName());
                streamWriter.writeAttribute(Attribute.NAME.getLocalName(), alias);
            }
        }
        if(accessLog != null) {
            streamWriter.writeStartElement(Element.ACCESS_LOG.getLocalName());
            accessLog.writeContent(streamWriter);
        }
        if(rewrite != null) {
            streamWriter.writeStartElement(Element.REWRITE.getLocalName());
            rewrite.writeContent(streamWriter);
        }
        streamWriter.writeEndElement();
    }

}
