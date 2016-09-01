/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.wise;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;

import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import static org.wildfly.extension.wise.WiseLogger.ROOT_LOGGER;

/**
 * Extension for subsystem that deploys wise.war.
 *
 * @author rsearls
 */
public class WiseExtension implements Extension {

   public static final String SUBSYSTEM_NAME = "wise";
   public static final String NAMESPACE = "urn:jboss:domain:wise:1.0";

   private static final WiseSubsystemParser parser = new WiseSubsystemParser();
   private static final PathElement PATH_SUBSYSTEM = PathElement.pathElement(SUBSYSTEM,
      SUBSYSTEM_NAME);


   private static final String RESOURCE_NAME = WiseExtension.class.getPackage().getName()
      + ".LocalDescriptions";

   private static final ModelVersion VERSION = ModelVersion.create(1,0,0);

   static StandardResourceDescriptionResolver getResourceDescriptionResolver(
      final String... keyPrefix) {
      StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
      for (String kp : keyPrefix) {
         prefix.append('.').append(kp);
      }
      return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME,
         WiseExtension.class.getClassLoader(), true, false);
   }

   private static final ResourceDefinition WISE_SUBSYSTEM_RESOURCE = new
       SimpleResourceDefinition(
      PATH_SUBSYSTEM,
      getResourceDescriptionResolver(),
      WiseSubsystemAdd.INSTANCE,
      WiseSubsystemRemove.INSTANCE);

   /** {@inheritDoc} */
   @Override
   public void initialize(final ExtensionContext context) {
      ROOT_LOGGER.debugf("Activating Wise Extension");
      final SubsystemRegistration subsystem = context.registerSubsystem(
         SUBSYSTEM_NAME, VERSION);
      final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(
         WISE_SUBSYSTEM_RESOURCE);
      registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION,
         GenericSubsystemDescribeHandler.INSTANCE, false);
      subsystem.registerXMLElementWriter(parser);
   }

   /** {@inheritDoc} */
   @Override
   public void initializeParsers(final ExtensionParsingContext context) {
      context.setSubsystemXmlMapping(SUBSYSTEM_NAME, WiseExtension.NAMESPACE, parser);
   }

   private static ModelNode createAddSubSystemOperation() {
      final ModelNode subsystem = new ModelNode();
      subsystem.get(OP).set(ADD);
      subsystem.get(OP_ADDR).add(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME);
      return subsystem;
   }

   static class WiseSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>,
      XMLElementWriter<SubsystemMarshallingContext> {

      /** {@inheritDoc} */
      @Override
      public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> list)
         throws XMLStreamException {
         // Require no attributes or content
         requireNoAttributes(reader);
         requireNoContent(reader);
         list.add(createAddSubSystemOperation());
      }

      /** {@inheritDoc} */
      @Override
      public void writeContent(final XMLExtendedStreamWriter streamWriter, final
         SubsystemMarshallingContext context) throws XMLStreamException {
         //TODO seems to be a problem with empty elements cleaning up the queue in FormattingXMLStreamWriter.runAttrQueue
         //context.startSubsystemElement(NewWeldExtension, true);
         context.startSubsystemElement(WiseExtension.NAMESPACE, false);
         streamWriter.writeEndElement();
      }

   }
}