/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
