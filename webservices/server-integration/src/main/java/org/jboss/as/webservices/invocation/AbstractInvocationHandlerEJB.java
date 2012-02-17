/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.webservices.invocation;

import static org.jboss.as.webservices.WSMessages.MESSAGES;
import static org.jboss.as.webservices.metadata.model.EJBEndpoint.EJB_COMPONENT_VIEW_NAME;
import static org.jboss.as.webservices.util.ASHelper.getMSCService;

import java.lang.reflect.Method;
import java.util.Collection;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.naming.ManagedReference;
import org.jboss.invocation.InterceptorContext;
import org.jboss.msc.service.ServiceName;
import org.jboss.ws.common.invocation.AbstractInvocationHandler;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.invocation.Invocation;

/**
 * Invocation abstraction for both EJB3 and  EJB21 endpoints.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
abstract class AbstractInvocationHandlerEJB extends AbstractInvocationHandler {

   /** EJB component view name */
   private ServiceName ejbComponentViewName;

   /** EJB component view */
   private volatile ComponentView ejbComponentView;
   private volatile ManagedReference reference;

   /**
    * Initializes EJB component name.
    *
    * @param endpoint web service endpoint
    */
   public void init(final Endpoint endpoint) {
       ejbComponentViewName = (ServiceName) endpoint.getProperty(EJB_COMPONENT_VIEW_NAME);
       if (ejbComponentViewName == null) throw MESSAGES.missingEjbComponentViewName();
   }

   /**
    * Gets EJB container lazily.
    *
    * @return EJB container
    */
   private ComponentView getComponentView() {
       //we need to check both, otherwise it is possible for
       //ejbComponentView to be initialized before reference
      if (ejbComponentView == null || reference == null) {
         synchronized(this) {
            if (ejbComponentView == null) {
               ejbComponentView = getMSCService(ejbComponentViewName, ComponentView.class);
               if (ejbComponentView == null) {
                  throw MESSAGES.cannotFindEjbView(ejbComponentViewName);
               }
                try {
                    reference = ejbComponentView.createInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
         }
      }
      return ejbComponentView;
   }

   /**
    * Invokes EJB endpoint.
    *
    * @param endpoint EJB endpoint
    * @param wsInvocation web service invocation
    * @throws Exception if any error occurs
    */
   public void invoke(final Endpoint endpoint, final Invocation wsInvocation) throws Exception {
      try {
         // prepare for invocation
         onBeforeInvocation(wsInvocation);
         // prepare invocation data
         final ComponentView componentView = getComponentView();
         final Method method = getEJBMethod(wsInvocation.getJavaMethod(), componentView.getViewMethods());
         final InterceptorContext context = new InterceptorContext();
         prepareForInvocation(context, wsInvocation);
         context.setMethod(method);
         context.setParameters(wsInvocation.getArgs());
         context.setTarget(reference.getInstance());
         context.putPrivateData(Component.class, componentView.getComponent());
         context.putPrivateData(ComponentView.class, componentView);
          // invoke method
         final Object retObj = componentView.invoke(context);
         // set return value
         wsInvocation.setReturnValue(retObj);
      }
      catch (Throwable t) {
         log.error("Method invocation failed with exception: " + t.getMessage(), t);
         handleInvocationException(t);
      }
      finally {
         onAfterInvocation(wsInvocation);
      }
   }

   /**
    * Translates SEI method to EJB view method.
    *
    * @param seiMethod SEI method
    * @param viewMethods EJB view methods
    * @return matching EJB view method
    */
   private Method getEJBMethod(final Method seiMethod, final Collection<Method> viewMethods) {
       for (final Method viewMethod : viewMethods) {
           if (matches(seiMethod, viewMethod)) {
               return viewMethod;
           }
       }
       throw new IllegalStateException();
   }

   /**
    * Compares two methods if they are identical.
    *
    * @param seiMethod reference method
    * @param viewMethod target method
    * @return true if they match, false otherwise
    */
   private boolean matches(final Method seiMethod, final Method viewMethod) {
       if (!seiMethod.getName().equals(viewMethod.getName())) return false;
       final Class<?>[] sourceParams = seiMethod.getParameterTypes();
       final Class<?>[] targetParams = viewMethod.getParameterTypes();
       if (sourceParams.length != targetParams.length) return false;
       for (int i = 0; i < sourceParams.length; i++) {
           if (!sourceParams[i].equals(targetParams[i])) return false;
       }
       return true;
   }

   protected void prepareForInvocation(final InterceptorContext context, final Invocation wsInvocation) {
       // does nothing
   }

}
