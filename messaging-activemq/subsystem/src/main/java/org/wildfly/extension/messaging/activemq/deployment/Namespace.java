/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.deployment;

import java.util.HashMap;
import java.util.Map;

/**
 * An enumeration of the supported Messaging deployment namespaces
 *
 * @author Stuart Douglas
 */
public enum Namespace {
   // must be first
   UNKNOWN(null),

   MESSAGING_DEPLOYMENT_1_0("urn:jboss:messaging-activemq-deployment:1.0"),
   ;


   private final String name;

   Namespace(final String name) {
      this.name = name;
   }

   /**
    * Get the URI of this namespace.
    *
    * @return the URI
    */
   public String getUriString() {
      return name;
   }

   private static final Map<String, Namespace> MAP;

   static {
      final Map<String, Namespace> map = new HashMap<String, Namespace>();
      for (Namespace namespace : values()) {
         final String name = namespace.getUriString();
         if (name != null) map.put(name, namespace);
      }
      MAP = map;
   }

   public static Namespace forUri(String uri) {
      final Namespace element = MAP.get(uri);
      return element == null ? UNKNOWN : element;
   }
}
