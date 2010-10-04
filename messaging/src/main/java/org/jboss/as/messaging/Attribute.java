package org.jboss.as.messaging;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public enum Attribute {
   UNKNOWN(null),
   /* Messaging 1.0 attributes, in alpha order */
   CONSUME_NAME("consume"),
   CREATEDURABLEQUEUE_NAME("createDurableQueue"),
   CREATE_NON_DURABLE_QUEUE_NAME("createNonDurableQueue"),
   CREATETEMPQUEUE_NAME("createTempQueue"),
   DELETE_NON_DURABLE_QUEUE_NAME("deleteNonDurableQueue"),
   DELETEDURABLEQUEUE_NAME("deleteDurableQueue"),
   DELETETEMPQUEUE_NAME("deleteTempQueue"),
   KEY("key"),
   MANAGE_NAME("manage"),
   MATCH("match"),
   NAME("name"),
   ROLES_ATTR_NAME("roles"),
   SEND_NAME("send"),
   TYPE_ATTR_NAME("type"),
   VALUE("value"),
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

   @Override
   public String toString() {
      return getLocalName();
   }
}
