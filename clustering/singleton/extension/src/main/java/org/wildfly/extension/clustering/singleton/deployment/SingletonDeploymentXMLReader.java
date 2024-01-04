/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton.deployment;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Parses singleton deployment configuration from XML.
 * @author Paul Ferraro
 */
public class SingletonDeploymentXMLReader implements XMLElementReader<MutableSingletonDeploymentConfiguration> {

    @SuppressWarnings("unused")
    private final SingletonDeploymentSchema schema;

    public SingletonDeploymentXMLReader(SingletonDeploymentSchema schema) {
        this.schema = schema;
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, MutableSingletonDeploymentConfiguration config) throws XMLStreamException {

        for (int i = 0; i < reader.getAttributeCount(); ++i) {
            String value = reader.getAttributeValue(i);
            switch (reader.getAttributeLocalName(i)) {
                case "policy": {
                    config.setPolicy(value);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        ParseUtils.requireNoContent(reader);
    }
}
