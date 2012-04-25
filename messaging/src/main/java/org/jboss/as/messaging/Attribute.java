package org.jboss.as.messaging;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;

/**
 *
 */
public enum Attribute {
   UNKNOWN((String) null),
   /* Messaging 1.0 attributes, in alpha order */
   ALLOW_DIRECT_CONNECTIONS_ONLY(CommonAttributes.ALLOW_DIRECT_CONNECTIONS_ONLY),
   // backup-connector-name is no longer used by HornetQ configuration
   @Deprecated
   BACKUP_CONNECTOR_NAME("backup-connector-name"),
   CONNECTOR_NAME(CommonAttributes.CONNECTOR_NAME),
   DISCOVERY_GROUP_NAME(CommonAttributes.DISCOVERY_GROUP_NAME),
   KEY(CommonAttributes.KEY),
   MATCH(CommonAttributes.MATCH),
   NAME(CommonAttributes.NAME),
   PATH(CommonAttributes.PATH),
   RELATIVE_TO(CommonAttributes.RELATIVE_TO),
   ROLES_ATTR_NAME(CommonAttributes.ROLES_ATTR_NAME),
   SERVER_ID(CommonAttributes.SERVER_ID),
   SOCKET_BINDING(CommonAttributes.SOCKET_BINDING),
   STRING(CommonAttributes.STRING),
   TYPE_ATTR_NAME(CommonAttributes.TYPE_ATTR_NAME),
   VALUE(CommonAttributes.VALUE);

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
