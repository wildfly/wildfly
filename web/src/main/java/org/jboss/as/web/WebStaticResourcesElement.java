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

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractModelElement;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * The configuration element for serving static resources in a web application.
 *
 * @author Emanuel Muckenhuber
 * @author Jean-Frederic Clere
 */
public class WebStaticResourcesElement extends AbstractModelElement<WebStaticResourcesElement> {

    /** The serialVersionUID */
    private static final long serialVersionUID = 7112890068879082292L;

    private Boolean listings;
    private Integer sendfile;
    private String fileEncoding;
    private Boolean readOnly;
    private Boolean webDav;
    private String secret;
    private Integer maxDepth;
    private Boolean disabled;

    protected WebStaticResourcesElement() {
        //
    }

    public Boolean isListings() {
        return listings;
    }

    public void setListings(Boolean listings) {
        this.listings = listings;
    }

    public Integer getSendfileSize() {
        return sendfile;
    }

    public void setSendfileSize(Integer sendfile) {
        this.sendfile = sendfile;
    }

    public String getFileEncoding() {
        return fileEncoding;
    }


    public void setFileEncoding(String fileEncoding) {
        this.fileEncoding = fileEncoding;
    }


    public Boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(Boolean readOnly) {
        this.readOnly = readOnly;
    }

    public Boolean isWebDav() {
        return webDav;
    }

    public void setWebDav(Boolean webDav) {
        this.webDav = webDav;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Integer getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(Integer maxDepth) {
        this.maxDepth = maxDepth;
    }

    public Boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    @Override
    protected Class<WebStaticResourcesElement> getElementClass() {
        return WebStaticResourcesElement.class;
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {

        writeAttribute(Attribute.LISTINGS, listings, streamWriter);
        writeAttribute(Attribute.SENDFILE, sendfile, streamWriter);
        writeAttribute(Attribute.FILE_ENCONDING, fileEncoding, streamWriter);
        writeAttribute(Attribute.READ_ONLY, readOnly, streamWriter);
        writeAttribute(Attribute.WEBDAV, webDav, streamWriter);
        writeAttribute(Attribute.SECRET, secret, streamWriter);
        writeAttribute(Attribute.MAX_DEPTH, maxDepth, streamWriter);
        writeAttribute(Attribute.DISABLED, disabled, streamWriter);

        streamWriter.writeEndElement();
    }

    static void writeAttribute(Attribute attribute, Integer content, XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        if(content != null) {
            writeAttribute(attribute, content.toString(), streamWriter);
        }
    }

    static void writeAttribute(Attribute attribute, Boolean content, XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        if(content != null) {
            writeAttribute(attribute, content.toString(), streamWriter);
        }
    }

    static void writeAttribute(Attribute attribute, String content, XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        if(content != null) {
            streamWriter.writeAttribute(attribute.getLocalName(), content);
        }
    }

}
