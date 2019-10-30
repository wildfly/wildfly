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

import java.util.HashMap;
import java.util.Map;

/**
 * Extension of {@link org.jboss.jca.common.api.metadata.common.Security} with added Elytron support.
 *
 * @author Flavia Rainone
 */
public interface Security extends org.jboss.jca.common.api.metadata.common.Security, SecurityMetadata {
   /**
    *
    * A Tag.
    *
    */
   enum Tag
   {
      // new Elytron tags
      /**
       * elytron-enabled tag
       */
      ELYTRON_ENABLED("elytron-enabled"),
      /**
       * authentication-context TAG
       */
      AUTHENTICATION_CONTEXT("authentication-context"),
      /**
       * authentication-context-and-application TAG
       */
      AUTHENTICATION_CONTEXT_AND_APPLICATION("authentication-context-and-application"),

      // tags copied from original tag class
      /** always first
       *
       */
      UNKNOWN(null),

      /**
       * security-domain tag
       */
      SECURITY_DOMAIN("security-domain"),

      /**
       * security-domain-and-application TAG
       */
      SECURITY_DOMAIN_AND_APPLICATION("security-domain-and-application"),

      /**
       * application
       */
      APPLICATION("application");


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

      private static final Map<String, Tag> MAP;

      static {
         final Map<String, Tag> map = new HashMap<>();
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
