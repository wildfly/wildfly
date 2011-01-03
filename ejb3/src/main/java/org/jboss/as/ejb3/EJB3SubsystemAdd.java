/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3;

import org.jboss.as.model.AbstractSubsystemAdd;
import org.jboss.as.model.BootUpdateContext;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;

/**
 * Add the EJB 3 subsystem directive.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class EJB3SubsystemAdd extends AbstractSubsystemAdd<EJB3SubsystemElement> {
   protected EJB3SubsystemAdd(final String namespaceUri) {
      super(namespaceUri);
   }

   @Override
   protected <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
      throw new RuntimeException("NYI: org.jboss.as.ejb3.EJB3SubsystemAdd.applyUpdate");
   }

   @Override
   protected void applyUpdateBootAction(BootUpdateContext updateContext) {
      // add the metadata parser deployment processor
      // TODO
      // add the real deployment processor
      // TODO: add the proper deployment processors
      // updateContext.addDeploymentProcessor(processor, priority);

      super.applyUpdateBootAction(updateContext);
   }

   @Override
   protected EJB3SubsystemElement createSubsystemElement() {
      return new EJB3SubsystemElement();
   }
}
