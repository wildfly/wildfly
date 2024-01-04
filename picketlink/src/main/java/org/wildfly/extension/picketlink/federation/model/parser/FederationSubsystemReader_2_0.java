/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.picketlink.federation.model.parser;

import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.federation.model.keystore.KeyResourceDefinition;
import org.wildfly.extension.picketlink.federation.model.keystore.KeyStoreProviderResourceDefinition;

import javax.xml.stream.XMLStreamException;
import java.util.List;

import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.wildfly.extension.picketlink.common.model.ModelElement.COMMON_NAME;
import static org.wildfly.extension.picketlink.common.model.ModelElement.KEY;
import static org.wildfly.extension.picketlink.common.model.ModelElement.KEY_STORE;

/**
 * <p> XML Reader for the subsystem schema, version 2.0. </p>
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class FederationSubsystemReader_2_0 extends AbstractFederationSubsystemReader {

    protected void parseKeyStore(XMLExtendedStreamReader reader, ModelNode parentNode, List<ModelNode> addOperations) throws XMLStreamException {
        ModelNode identityProviderNode = parseConfig(reader, KEY_STORE, null, parentNode,
            KeyStoreProviderResourceDefinition.INSTANCE.getAttributes(), addOperations);

        parseElement(new ElementParser() {
            @Override
            public void parse(final XMLExtendedStreamReader reader, final ModelElement element, final ModelNode parentNode,
                List<ModelNode> addOperations) throws XMLStreamException {
                switch (element) {
                    case KEY:
                        parseConfig(reader, KEY, COMMON_NAME.getName(), parentNode,
                            KeyResourceDefinition.INSTANCE.getAttributes(), addOperations);
                        break;
                    default:
                        throw unexpectedElement(reader);
                }
            }
        }, KEY_STORE, identityProviderNode, reader, addOperations);
    }

}
