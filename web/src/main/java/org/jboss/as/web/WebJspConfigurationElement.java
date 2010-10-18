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
 * Web jsp container configuration.
 *
 * @author Emanuel Muckenhuber
 */
public class WebJspConfigurationElement extends AbstractModelElement<WebJspConfigurationElement> {

    /** The serialVersionUID */
    private static final long serialVersionUID = -2092852855302799521L;

    private Boolean development; // false;
    private Boolean keepGenerated; // true;
    private Boolean trimSpaces; // false;
    private Boolean tagPooling; // true;
    private Boolean mappedFile; // true;
    private Integer checkInterval; // 0;
    private Integer modificationTestInterval;  // 4;
    private Boolean recompileOnFail; // false;
    private Boolean smap; // true;
    private Boolean dumpSmap; // false;
    private Boolean generateStringsAsCharArrays; // false;
    private Boolean errorOnInvalidClassAttribute; // false;
    private String scratchDir;
    private String sourceVM; // "1.5";
    private String targetVM; // "1.5";
    private String javaEncoding; // "UTF-8";
    private Boolean XPoweredBy; // true;
    private Boolean displaySourceFragment; // true;
    private Boolean disabled; // false;

    protected WebJspConfigurationElement() {
        //
    }

    public Boolean isDevelopment() {
        return development;
    }

    public void setDevelopment(Boolean development) {
        this.development = development;
    }

    public Boolean isKeepGenerated() {
        return keepGenerated;
    }

    public void setKeepGenerated(Boolean keepGenerated) {
        this.keepGenerated = keepGenerated;
    }

    public Boolean isTrimSpaces() {
        return trimSpaces;
    }

    public void setTrimSpaces(Boolean trimSpaces) {
        this.trimSpaces = trimSpaces;
    }

    public Boolean isTagPooling() {
        return tagPooling;
    }

    public void setTagPooling(Boolean tagPooling) {
        this.tagPooling = tagPooling;
    }

    public Boolean isMappedFile() {
        return mappedFile;
    }

    public void setMappedFile(Boolean mappedFile) {
        this.mappedFile = mappedFile;
    }

    public Integer getCheckInterval() {
        return checkInterval;
    }

    public void setCheckInterval(Integer checkInterval) {
        this.checkInterval = checkInterval;
    }

    public Integer getModificationTestInterval() {
        return modificationTestInterval;
    }

    public void setModificationTestInterval(Integer modificationTestInterval) {
        this.modificationTestInterval = modificationTestInterval;
    }

    public Boolean isRecompileOnFail() {
        return recompileOnFail;
    }

    public void setRecompileOnFail(Boolean recompileOnFail) {
        this.recompileOnFail = recompileOnFail;
    }

    public Boolean isSmap() {
        return smap;
    }

    public void setSmap(Boolean smap) {
        this.smap = smap;
    }

    public Boolean isDumpSmap() {
        return dumpSmap;
    }

    public void setDumpSmap(Boolean dumpSmap) {
        this.dumpSmap = dumpSmap;
    }

    public Boolean isGenerateStringsAsCharArrays() {
        return generateStringsAsCharArrays;
    }

    public void setGenerateStringsAsCharArrays(Boolean generateStringsAsCharArrays) {
        this.generateStringsAsCharArrays = generateStringsAsCharArrays;
    }

    public Boolean isErrorOnInvalidClassAttribute() {
        return errorOnInvalidClassAttribute;
    }

    public void setErrorOnInvalidClassAttribute(Boolean errorOnInvalidClassAttribute) {
        this.errorOnInvalidClassAttribute = errorOnInvalidClassAttribute;
    }

    public String getScratchDir() {
        return scratchDir;
    }

    public void setScratchDir(String scratchDir) {
        this.scratchDir = scratchDir;
    }

    public String getSourceVM() {
        return sourceVM;
    }

    public void setSourceVM(String sourceVM) {
        this.sourceVM = sourceVM;
    }

    public String getTargetVM() {
        return targetVM;
    }

    public void setTargetVM(String targetVM) {
        this.targetVM = targetVM;
    }

    public String getJavaEncoding() {
        return javaEncoding;
    }

    public void setJavaEncoding(String javaEncoding) {
        this.javaEncoding = javaEncoding;
    }

    public Boolean isXPoweredBy() {
        return XPoweredBy;
    }

    public void setXPoweredBy(Boolean xPoweredBy) {
        XPoweredBy = xPoweredBy;
    }

    public Boolean isDisplaySourceFragment() {
        return displaySourceFragment;
    }

    public void setDisplaySourceFragment(Boolean displaySourceFragment) {
        this.displaySourceFragment = displaySourceFragment;
    }

    public Boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }


    @Override
    protected Class<WebJspConfigurationElement> getElementClass() {
        return WebJspConfigurationElement.class;
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        writeAttribute(Attribute.DEVELOPMENT, development, streamWriter);
        writeAttribute(Attribute.KEEP_GENERATED, keepGenerated, streamWriter);
        writeAttribute(Attribute.TRIM_SPACES, trimSpaces, streamWriter);
        writeAttribute(Attribute.TAG_POOLING, tagPooling, streamWriter);
        writeAttribute(Attribute.MAPPED_FILE, mappedFile, streamWriter);
        writeAttribute(Attribute.CHECK_INTERVAL, checkInterval, streamWriter);
        writeAttribute(Attribute.MODIFIFICATION_TEST_INTERVAL, modificationTestInterval, streamWriter);
        writeAttribute(Attribute.RECOMPILE_ON_FAIL, recompileOnFail, streamWriter);
        writeAttribute(Attribute.SMAP, smap, streamWriter);
        writeAttribute(Attribute.DUMP_SMAP, dumpSmap, streamWriter);
        writeAttribute(Attribute.GENERATE_STRINGS_AS_CHAR_ARRAYS, generateStringsAsCharArrays, streamWriter);
        writeAttribute(Attribute.ERROR_ON_USE_BEAN_INVALID_CLASS_ATTRIBUT, errorOnInvalidClassAttribute, streamWriter);
        writeAttribute(Attribute.SCRATCH_DIR, scratchDir, streamWriter);
        writeAttribute(Attribute.SOURCE_VM, sourceVM, streamWriter);
        writeAttribute(Attribute.TARGET_VM, targetVM, streamWriter);
        writeAttribute(Attribute.JAVA_ENCODING, javaEncoding, streamWriter);
        writeAttribute(Attribute.X_POWERED_BY, XPoweredBy, streamWriter);
        writeAttribute(Attribute.DISPLAY_SOOURCE_FRAGMENT, displaySourceFragment, streamWriter);
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
