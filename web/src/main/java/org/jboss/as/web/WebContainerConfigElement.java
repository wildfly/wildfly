/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractModelElement;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * The web container configuration.
 *
 * @author Jean-Frederic Clere
 */
public class WebContainerConfigElement extends AbstractModelElement<WebContainerConfigElement> {

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    /** The resource serving configuration. */
    private WebStaticResourcesElement staticResources;
    private WebJspConfigurationElement jspConfiguration;
    private Map<String, String> mimeMappings;
    private Collection<String> welcomeFiles = new TreeSet<String>();

    protected WebContainerConfigElement() {
        //
    }

    public WebStaticResourcesElement getStaticResources() {
        return staticResources;
    }

    public void setStaticResources(WebStaticResourcesElement resourceServing) {
        this.staticResources = resourceServing;
    }

    public WebJspConfigurationElement getJspConfiguration() {
        return jspConfiguration;
    }

    public void setJspConfiguration(WebJspConfigurationElement jspConfiguration) {
        this.jspConfiguration = jspConfiguration;
    }

    public Map<String, String> getMimeMappings() {
        return mimeMappings;
    }

    public void setMimeMappings(Map<String, String> mimeMappings) {
        this.mimeMappings = mimeMappings;
    }

    public Collection<String> getWelcomeFiles() {
        return welcomeFiles;
    }

    public void setWelcomeFiles(Collection<String> welcomeFiles) {
        this.welcomeFiles = welcomeFiles;
    }

    /** {@inheritDoc} */
    protected Class<WebContainerConfigElement> getElementClass() {
        return WebContainerConfigElement.class;
    }

    /** {@inheritDoc} */
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        if(staticResources != null) {
            streamWriter.writeStartElement(Element.STATIC_RESOURCES.getLocalName());
            staticResources.writeContent(streamWriter);
        }
        if(jspConfiguration != null) {
            streamWriter.writeStartElement(Element.JSP_CONFIGURATION.getLocalName());
            jspConfiguration.writeContent(streamWriter);
        }
        if(mimeMappings != null && !mimeMappings.isEmpty()) {
            for(final Entry<String, String> entry : mimeMappings.entrySet()) {
                streamWriter.writeEmptyElement(Element.MIME_MAPPING.getLocalName());
                streamWriter.writeAttribute(Attribute.NAME.getLocalName(), entry.getKey());
                streamWriter.writeAttribute(Attribute.VALUE.getLocalName(), entry.getValue());
            }
        }
        if(welcomeFiles != null && !welcomeFiles.isEmpty()) {
            for(final String file : welcomeFiles) {
                streamWriter.writeStartElement(Element.WELCOME_FILE.getLocalName());
                streamWriter.writeCharacters(file);
                streamWriter.writeEndElement();
            }
        }
        streamWriter.writeEndElement();
    }

}
