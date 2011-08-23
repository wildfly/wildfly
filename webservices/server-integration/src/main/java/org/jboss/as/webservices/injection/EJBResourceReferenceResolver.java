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

import org.jboss.as.ejb3.injection.EjbResourceResolver;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.ws.common.injection.resolvers.AbstractReferenceResolver;

/**
 * EJB reference resolver.
 *
 * @author <a href="mailto:richard.opalka@jboss.org">Richard Opalka</a>
 */
final class EJBResourceReferenceResolver extends AbstractReferenceResolver<EJB> {

   /**
    * Deployment unit used for resolving process.
    */
   private final DeploymentUnit unit;

   /**
    * Constructor.
    *
    * @param unit deployment unit
    */
   EJBResourceReferenceResolver(final DeploymentUnit unit) {
      super(EJB.class);
      this.unit = unit;
   }

   /**
    * @see org.jboss.ws.common.injection.resolvers.AbstractReferenceResolver#resolveField(java.lang.reflect.Field)
    *
    * @param field to be resolved
    * @return JNDI name of referenced EJB object
    */
   @Override
   protected String resolveField(final Field field) {
       return EjbResourceResolver.getInstance().resolve(unit, field);
   }

   /**
    * @see org.jboss.ws.common.injection.resolvers.AbstractReferenceResolver#resolveMethod(java.lang.reflect.Method)
    *
    * @param method to be resolved
    * @return JNDI name of referenced EJB object
    */
   @Override
   protected String resolveMethod(final Method method) {
      return EjbResourceResolver.getInstance().resolve(unit, method);
   }

}
