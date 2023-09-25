/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.metadata.api.common;

import org.jboss.as.controller.security.CredentialReference;
import org.jboss.jca.common.api.metadata.common.SecurityMetadata;

import java.util.HashMap;
import java.util.Map;

/**
 * Extension of {@link org.jboss.jca.common.api.metadata.common.Credential} with added Elytron support.
 *
 * @author Flavia Rainone
 */
public interface Credential extends org.jboss.jca.common.api.metadata.common.Credential, SecurityMetadata {
   /**
    *
    * A Tag.
    *
    */
   public enum Attribute
   {
      /** unknown attribute
       *
       */
      UNKNOWN(null),
      /**
       * userName attribute
       */
      USER_NAME("user-name"),
      /**
       * password attribute
       */
      PASSWORD("password");

      private String name;

      /**
       *
       * Create a new Tag.
       *
       * @param name a name
       */
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

      /**
       * {@inheritDoc}
       */
      public String toString() {
         return name;
      }

      private static final Map<String, Credential.Attribute> MAP;

      static {
         final Map<String, Credential.Attribute> map = new HashMap<>();
         for (Credential.Attribute element : values()) {
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
      Credential.Attribute value(String v) {
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
      public static Credential.Attribute forName(String localName) {
         final Credential.Attribute element = MAP.get(localName);
         return element == null ? UNKNOWN.value(localName) : element;
      }
   }


   enum Tag
   {
      // new Elytron tag
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
      CREDENTIAL_REFERENCE(CredentialReference.CREDENTIAL_REFERENCE),

      // tags copied from original tag class
      /** always first
       *
       */
      UNKNOWN(null),

      /**
       * userName tag
       */
      @Deprecated
      USER_NAME("user-name"),
      /**
       * password tag
       */
      @Deprecated
      PASSWORD("password"),

      /**
       * security-domain tag
       */
      SECURITY_DOMAIN("security-domain");


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
