/*
 * Copyright 2017 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.connector.metadata.api.common;

import org.jboss.as.controller.security.CredentialReference;

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
      USER_NAME("user-name"),
      /**
       * password tag
       */
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
         final java.util.Map<String, Credential.Tag> map = new java.util.HashMap<>();
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
