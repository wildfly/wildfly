package org.jboss.as.messaging;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public enum Attribute {
   UNKNOWN(null),
   /* Messaging 1.0 attributes, in alpha order */
   CONSUME_NAME(CommonAttributes.CONSUME_NAME),
   CREATEDURABLEQUEUE_NAME(CommonAttributes.CREATEDURABLEQUEUE_NAME),
   CREATE_NON_DURABLE_QUEUE_NAME(CommonAttributes.CREATE_NON_DURABLE_QUEUE_NAME),
   CREATETEMPQUEUE_NAME(CommonAttributes.CREATETEMPQUEUE_NAME),
   DELETE_NON_DURABLE_QUEUE_NAME(CommonAttributes.DELETE_NON_DURABLE_QUEUE_NAME),
   DELETEDURABLEQUEUE_NAME(CommonAttributes.DELETEDURABLEQUEUE_NAME),
   DELETETEMPQUEUE_NAME(CommonAttributes.DELETETEMPQUEUE_NAME),
   KEY(CommonAttributes.KEY),
   MANAGE_NAME(CommonAttributes.MANAGE_NAME),
   MATCH(CommonAttributes.MATCH),
   NAME(CommonAttributes.NAME),
   PATH(CommonAttributes.PATH),
   RELATIVE_TO(CommonAttributes.RELATIVE_TO),
   ROLES_ATTR_NAME(CommonAttributes.ROLES_ATTR_NAME),
   SEND_NAME(CommonAttributes.SEND_NAME),
   SERVER_ID(CommonAttributes.SERVER_ID),
   SOCKET_BINDING(CommonAttributes.SOCKET_BINDING),
   STRING(CommonAttributes.STRING),
   TYPE_ATTR_NAME(CommonAttributes.TYPE_ATTR_NAME),
   VALUE(CommonAttributes.VALUE),
   //JMS Stuff
   CONNECTOR_NAME(CommonAttributes.CONNECTOR_NAME),
   DISCOVERY_GROUP_NAME(CommonAttributes.DISCOVERY_GROUP_NAME),
   ;
   private final String name;

   Attribute(final String name) {
      this.name = name;
   }

   /**
    * Get the local name of this element.
    *
    * @return the local name
    */
   public String getLocalName() {
      return name;
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
