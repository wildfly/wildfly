/*
 * JBoss, Home of Professional Open Source
 * Copyright 2019, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.jaxrs;

import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

public class JaxrsSubsystemParser_2_0 implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

   private static final Pattern PATTERN_LIST = Pattern.compile(".+(,.+)*");
   private static final Pattern PATTERN_PROPERTY_LIST = Pattern.compile(".+:.+(,.+:.+)*");

   /**
    * {@inheritDoc}
    */
   @Override
   public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> list) throws XMLStreamException {
       // Require no attributes or content
       requireNoAttributes(reader);
       list.add(Util.createAddOperation(PathAddress.pathAddress(JaxrsExtension.SUBSYSTEM_PATH)));

       //Read the children
       while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
           if (!reader.getLocalName().equals("context-parameters")) {
               throw ParseUtils.unexpectedElement(reader);
           }
           while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
               if (reader.isStartElement()) {
                   readContextParameter(reader, list);
               }
           }
       }
   }

   private void readContextParameter(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
      if (!reader.getLocalName().equals("context-parameter")) {
          throw ParseUtils.unexpectedElement(reader);
      }

      String name = reader.getAttributeValue(0);
      String value = reader.getAttributeValue(1);
      if (PATTERN_PROPERTY_LIST.matcher(value).matches() || PATTERN_LIST.matcher(value).matches()) {
         String[] ss = value.split(",");
         ModelNode modelNode = new ModelNode();
         for (String s : ss) {
            modelNode.add(s.trim());
         }
         JaxrsContextParamHandler.getContextParameters().put(name, modelNode);
      } else {
         JaxrsContextParamHandler.getContextParameters().put(name, new ModelNode(value));
      }
      ParseUtils.requireNoContent(reader);
  }

   /**
    * {@inheritDoc}
    */
   @Override
   public void writeContent(final XMLExtendedStreamWriter streamWriter, final SubsystemMarshallingContext context) throws XMLStreamException {
       context.startSubsystemElement(JaxrsExtension.NAMESPACE_2_0, false);
       Map<String, ModelNode> contextParameters = JaxrsContextParamHandler.getContextParameters();
       if (contextParameters == null || contextParameters.size() == 0) {
          streamWriter.writeEndElement();
          return;
       }
       streamWriter.writeStartElement("context-parameters");
       for (Entry<String, ModelNode> entry : contextParameters.entrySet()) {
          if (ModelType.LIST.equals(entry.getValue().getType())) {
             if (entry.getValue().asList().size() > 0) {
                streamWriter.writeStartElement("context-parameter");
                streamWriter.writeAttribute("name", entry.getKey());
                StringBuffer sb = new StringBuffer();
                boolean first = true;
                for(ModelNode modelNode : entry.getValue().asList()) {
                   if (first) {
                      first = false;
                   } else {
                      sb.append(",");
                   }
                   sb.append(modelNode.asString());
                }
                streamWriter.writeAttribute("value", sb.toString());
                streamWriter.writeEndElement();
             }
          } else {
             streamWriter.writeStartElement("context-parameter");
             streamWriter.writeAttribute("name", entry.getKey());
             streamWriter.writeAttribute("value", entry.getValue().asString());
             streamWriter.writeEndElement();
          }
       }
       streamWriter.writeEndElement();
       streamWriter.writeEndElement();
   }
}
