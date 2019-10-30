/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.weld.deployment;


import java.net.URL;

import org.jboss.as.weld.logging.WeldLogger;
import org.jboss.metadata.property.PropertyReplacer;
import org.jboss.weld.xml.BeansXmlHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * {@link BeansXmlHandler} with AS7-specific logging and property replacement support.
 *
 * @author Jozef Hartinger
 *
 */
class PropertyReplacingBeansXmlHandler extends BeansXmlHandler {

    private static final String ROOT_ELEMENT_NAME = "beans";
    // See also https://www.w3.org/TR/xmlschema-1/#cvc-elt
    private static final String VALIDATION_ERROR_CODE_CVC_ELT_1 = "cvc-elt.1";

    private final PropertyReplacer replacer;

    public PropertyReplacingBeansXmlHandler(URL file, PropertyReplacer replacer) {
        super(file);
        this.replacer = replacer;
    }

    @Override
    public void warning(SAXParseException e) throws SAXException {
        WeldLogger.DEPLOYMENT_LOGGER.beansXmlValidationWarning(file, e.getLineNumber(), e.getMessage());
    }

    @Override
    public void error(SAXParseException e) throws SAXException {
        if (e.getMessage().startsWith(VALIDATION_ERROR_CODE_CVC_ELT_1) && e.getMessage().contains(ROOT_ELEMENT_NAME)) {
            // Ignore the errors we get when there is no schema defined
            return;
        }
        WeldLogger.DEPLOYMENT_LOGGER.beansXmlValidationError(file, e.getLineNumber(), e.getMessage());
    }

    @Override
    protected String interpolate(String text) {
        if(text == null) {
            return null;
        }
        return replacer.replaceProperties(text);
    }
}
