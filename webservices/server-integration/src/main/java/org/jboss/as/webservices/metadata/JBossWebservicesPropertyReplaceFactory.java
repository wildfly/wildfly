/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.metadata;

import static org.jboss.wsf.spi.util.StAXUtils.elementAsString;

import java.net.URL;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.metadata.property.PropertyReplacer;
import org.jboss.wsf.spi.metadata.webservices.JBossWebservicesFactory;

/**
 * @author <a href="mailto:ema@redhat.com">Jim Ma</a>
 */
public class JBossWebservicesPropertyReplaceFactory extends JBossWebservicesFactory {

    private PropertyReplacer replacer;


    public JBossWebservicesPropertyReplaceFactory(final URL descriptorURL, final PropertyReplacer propertyReplacer) {
        super(descriptorURL);
        replacer = propertyReplacer;
    }

    @Override
    public String getElementText(XMLStreamReader reader) throws XMLStreamException {
        String res = elementAsString(reader);
        if (res != null && replacer != null) {
            res = replacer.replaceProperties(res);
        }
        return res;
    }

}
