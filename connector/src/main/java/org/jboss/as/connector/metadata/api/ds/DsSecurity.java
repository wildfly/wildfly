/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.metadata.api.ds;

import org.jboss.jca.common.api.metadata.common.SecurityMetadata;

/**
 * Extension of {@link org.jboss.jca.common.api.metadata.ds.DsSecurity} with added Elytron support.
 *
 * @author Flavia Rainone
 */
public interface DsSecurity extends org.jboss.jca.common.api.metadata.ds.DsSecurity, SecurityMetadata {
   /**
    *
    * A Tag.
    *
    */
   enum Tag {
      // elytron tags
      /**
       * elytron-enabled tag
       */
      ELYTRON_ENABLED("elytron-enabled"),
      /**
       * authentication-context tag
       */
      AUTHENTICATION_CONTEXT("authentication-context"),

      /**
       * credential-reference tag
       */
      CREDENTIAL_REFERENCE("credential-reference"),

      // other tags, copied from original tag enum
      /** always first
       *
       */
      UNKNOWN(null),

      /**
       * userName tag
       */
      USER_NAME("user-name"),
      /**
       * password tag
       */
      PASSWORD("password"),

      /**
       * security-domain tag
       */
      SECURITY_DOMAIN("security-domain"),

      /**
       * reauth-plugin tag
       */
      REAUTH_PLUGIN("reauth-plugin");

      private String name;

      /**
       *
       * Create a new Tag.
       *
       * @param name a name
       */
      Tag(final String name) {
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

      /**
       * {@inheritDoc}
       */
      public String toString() {
         return name;
      }

      private static final java.util.Map<String, Tag> MAP;

      static {
         final java.util.Map<String, Tag> map = new java.util.HashMap<>();
         for (Tag element : values())
         {
            final String name = element.getLocalName();
            if (name != null)
               map.put(name, element);
         }
         MAP = map;
      }

      /**
       * Set the value
       * @param v The name
       * @return The value
       */
      Tag value(String v) {
         name = v;
         return this;
      }

      /**
       *
       * Static method to get enum instance given localName XsdString
       *
       * @param localName a XsdString used as localname (typically tag name as defined in xsd)
       * @return the enum instance
       */
      public static Tag forName(String localName) {
         final Tag element = MAP.get(localName);
         return element == null ? UNKNOWN.value(localName) : element;
      }
   }
}
