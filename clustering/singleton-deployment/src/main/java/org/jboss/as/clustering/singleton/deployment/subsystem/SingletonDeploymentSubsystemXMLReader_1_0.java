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

package org.jboss.as.clustering.singleton.deployment.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * @author Paul Ferraro
 */
public class SingletonDeploymentSubsystemXMLReader_1_0 implements XMLElementReader<List<ModelNode>> {

    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
        PathAddress subsystemAddress = PathAddress.pathAddress(SingletonDeploymentExtension.SUBSYSTEM_PATH);
        ModelNode subsystem = Util.createAddOperation(subsystemAddress);

        operations.add(subsystem);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case DEPLOYMENT_POLICY: {
                    operations.add(readDeploymentPolicy(reader, subsystemAddress));
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    protected ModelNode readDeploymentPolicy(XMLExtendedStreamReader reader, PathAddress address) throws XMLStreamException {
        ModelNode operation = Util.getEmptyOperation(ADD, null);
        final Set<Attribute> required = EnumSet.of(Attribute.NAME, Attribute.CACHE_CONTAINER);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            ParseUtils.requireNoNamespaceAttribute(reader, i);
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME: {
                    operation.get(OP_ADDR).set(address.append(ModelKeys.DEPLOYMENT_POLICY, value).toModelNode());
                    break;
                }
                case CACHE_CONTAINER: {
                    DeploymentPolicyResourceDefinition.CACHE_CONTAINER.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case PREFERRED_NODES: {
                    for (String node: reader.getListAttributeValue(i)) {
                        DeploymentPolicyResourceDefinition.PREFERRED_NODES.parseAndAddParameterElement(node, operation, reader);
                    }
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        if (!required.isEmpty()) {
            throw ParseUtils.missingRequired(reader, required);
        }

        ParseUtils.requireNoContent(reader);

        return operation;
    }
}
