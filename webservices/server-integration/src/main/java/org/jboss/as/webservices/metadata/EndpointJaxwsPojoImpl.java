/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.webservices.metadata;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class EndpointJaxwsPojoImpl implements EndpointJaxwsPojo {

   private final String pojoName;
   private final String pojoClassName;
   private final String urlPattern;
   private final boolean isDeclared;

   public EndpointJaxwsPojoImpl(final String pojoName, final String pojoClassName, final String urlPattern) {
      this.pojoName = pojoName;
      this.pojoClassName = pojoClassName;
      this.urlPattern = urlPattern;
      this.isDeclared = true;
   }

   public EndpointJaxwsPojoImpl(final String pojoClassName, final String urlPattern) {
       this.pojoName = pojoClassName;
       this.pojoClassName = pojoClassName;
       this.urlPattern = urlPattern;
       this.isDeclared = false;
   }

   public String getName() {
       return pojoName;
   }

   public String getClassName() {
       return pojoClassName;
   }

   public String getUrlPattern() {
       return urlPattern;
   }

   public boolean isDeclared() {
       return isDeclared;
   }

   public String toString() {
       final StringBuilder builder = new StringBuilder();
       builder.append("JAXWS POJO { name=").append(pojoName).append(", class=").append(pojoClassName).append(", urlPattern=").append(urlPattern).append("}");
       return builder.toString();
   }

}