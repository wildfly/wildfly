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

package org.jboss.as.messaging;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.messaging.jms.bridge.JMSBridgeDefinition;

/**
 *
 */
public enum Attribute {
   UNKNOWN((String) null),
   /* Messaging 1.0 attributes, in alpha order */
   ALLOW_DIRECT_CONNECTIONS_ONLY(ClusterConnectionDefinition.ALLOW_DIRECT_CONNECTIONS_ONLY),
   // backup-connector-name is no longer used by HornetQ configuration
   @Deprecated
   BACKUP_CONNECTOR_NAME("backup-connector-name"),
   CONNECTOR_NAME(CommonAttributes.CONNECTOR_NAME),
   CONNECTOR_REF(CommonAttributes.CONNECTOR_REF_STRING),
   HTTP_LISTENER(CommonAttributes.HTTP_LISTENER),
   KEY(CommonAttributes.KEY),
   MATCH(CommonAttributes.MATCH),
   NAME(CommonAttributes.NAME),
   PATH(ModelDescriptionConstants.PATH),
   RELATIVE_TO(PathDefinition.RELATIVE_TO),
   ROLES_ATTR_NAME(CommonAttributes.ROLES_ATTR_NAME),
   SERVER_ID(InVMTransportDefinition.SERVER_ID),
   SOCKET_BINDING(RemoteTransportDefinition.SOCKET_BINDING),
   STRING(CommonAttributes.STRING),
   TYPE_ATTR_NAME(CommonAttributes.TYPE_ATTR_NAME),
   VALUE(ConnectorServiceParamDefinition.VALUE),
   MODULE(JMSBridgeDefinition.MODULE);


   private final String name;
   private final AttributeDefinition definition;

   Attribute(final String name) {
      this.name = name;
       this.definition = null;
   }

   Attribute(final AttributeDefinition definition) {
       this.name = definition.getXmlName();
       this.definition = definition;
   }

   /**
    * Get the local name of this element.
    *
    * @return the local name
    */
   public String getLocalName() {
      return name;
   }

   public AttributeDefinition getDefinition() {
       return definition;
   }

   private static final Map<String, Attribute> MAP;

   static {
      final Map<String, Attribute> map = new HashMap<String, Attribute>();
      for (Attribute element : values()) {
         final String name = element.getLocalName();
         if (name != null) map.put(name, element);
      }
      MAP = map;
   }

   public static Attribute forName(String localName) {
      final Attribute element = MAP.get(localName);
      return element == null ? UNKNOWN : element;
   }

}
