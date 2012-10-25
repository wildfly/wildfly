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

import static org.jboss.as.clustering.singleton.deployment.subsystem.DeploymentPolicyResourceDefinition.CACHE_CONTAINER;
import static org.jboss.as.clustering.singleton.deployment.subsystem.DeploymentPolicyResourceDefinition.PREFERRED_NODES;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author Paul Ferraro
 */
public class SingletonDeploymentSubsystemXMLWriter implements XMLElementWriter<SubsystemMarshallingContext> {

    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(Namespace.CURRENT.getUri(), false);
        ModelNode model = context.getModelNode();
        if (model.isDefined()) {
            for (Property property: model.get(ModelKeys.DEPLOYMENT_POLICY).asPropertyList()) {
                writer.writeStartElement(Element.DEPLOYMENT_POLICY.getLocalName());

                String deploymentPolicyName = property.getName();
                writer.writeAttribute(Attribute.NAME.getLocalName(), deploymentPolicyName);

                ModelNode deploymentPolicy = property.getValue();

                CACHE_CONTAINER.marshallAsAttribute(deploymentPolicy, writer);
                PREFERRED_NODES.marshallAsElement(deploymentPolicy, false, writer);

                writer.writeEndElement();
            }
        }
        writer.writeEndElement();
    }
}
