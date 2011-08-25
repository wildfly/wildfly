/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.webservices.injection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.ejb.EJB;

import org.jboss.ws.common.injection.resolvers.AbstractReferenceResolver;

/**
 * EJB reference resolver.
 *
 * @author <a href="mailto:richard.opalka@jboss.org">Richard Opalka</a>
 */
final class EJBReferenceResolver extends AbstractReferenceResolver<EJB> {

   /**
    * Constructor.
    *
    * @param unit deployment unit
    */
   EJBReferenceResolver() {
      super(EJB.class);
   }

   /**
    * @see org.jboss.ws.common.injection.resolvers.AbstractReferenceResolver#resolveField(java.lang.reflect.Field)
    *
    * @param field to be resolved
    * @return JNDI name of referenced EJB object
    */
   @Override
   protected String resolveField(final Field field) {
       final String fieldName = field.getName();
       final EJB annotation = field.getAnnotation(EJB.class);

       return isEmpty(annotation.name()) ? field.getDeclaringClass().getName() + "/" + fieldName : annotation.name();
   }

   /**
    * @see org.jboss.ws.common.injection.resolvers.AbstractReferenceResolver#resolveMethod(java.lang.reflect.Method)
    *
    * @param method to be resolved
    * @return JNDI name of referenced EJB object
    */
   @Override
   protected String resolveMethod(final Method method) {
       final String methodName = method.getName();
       final String propertyName = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
       final EJB annotation = method.getAnnotation(EJB.class);

       return isEmpty(annotation.name()) ? method.getDeclaringClass().getName() + "/" + propertyName : annotation.name();
   }

   private static boolean isEmpty(final String s) {
       return s == null || s.length() == 0;
   }

}
