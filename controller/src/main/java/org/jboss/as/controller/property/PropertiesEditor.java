/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.controller.property;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;

/**
 * A property editor for {@link java.util.Properties}.
 *
 * @author Jason Dillon
 * @author Scott.Stark@jboss.org
 *
 */
@SuppressWarnings("unchecked")
public class PropertiesEditor extends TextPropertyEditorSupport {
    /**
     * Returns a Properties object initialized with current getAsText value interpretted as a .properties file contents. This
     * replaces any references of the form ${x} with the corresponding system property.
     *
     * @return a Properties object
     * @throws NestedRuntimeException An IOException occured.
     */
    public Object getValue() {
        try {
            // Load the current key=value properties into a Properties object
            String propsText = getAsText();
            Properties rawProps = new Properties(System.getProperties());
            ByteArrayInputStream bais = new ByteArrayInputStream(propsText.getBytes());
            rawProps.load(bais);
            // Now go through the rawProps and replace any ${x} refs
            Properties props = new Properties();
            Iterator keys = rawProps.keySet().iterator();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                String value = rawProps.getProperty(key);
                String value2 = StringPropertyReplacer.replaceProperties(value, rawProps);
                props.setProperty(key, value2);
            }
            rawProps.clear();

            return props;
        } catch (IOException e) {
            throw new NestedRuntimeException(e);
        }
    }
}
