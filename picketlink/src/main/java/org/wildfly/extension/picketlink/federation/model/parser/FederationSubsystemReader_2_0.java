/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
