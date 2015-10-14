/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.weld;

import org.jboss.staxmapper.XMLExtendedStreamReader;

abstract class AbstractWeldJBossAllParser {

    protected Boolean getAttributeAsBoolean(XMLExtendedStreamReader reader, String attributeName) {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            final String name = reader.getAttributeLocalName(i);
            final String value = reader.getAttributeValue(i);
            if (attributeName.equals(name)) {
                return Boolean.valueOf(value);
            }
        }
        return null;
    }
}
