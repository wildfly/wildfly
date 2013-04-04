/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
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
