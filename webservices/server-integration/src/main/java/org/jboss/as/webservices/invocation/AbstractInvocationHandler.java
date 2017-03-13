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

import static org.jboss.as.webservices.metadata.model.AbstractEndpoint.COMPONENT_VIEW_NAME;
import static org.jboss.as.webservices.util.ASHelper.getMSCService;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collection;
import java.util.concurrent.Callable;

import javax.management.MBeanException;
import javax.xml.ws.soap.SOAPFaultException;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.webservices.injection.WSComponent;
import org.jboss.as.webservices.logging.WSLogger;
import org.jboss.invocation.InterceptorContext;
import org.jboss.msc.service.ServiceName;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.deployment.EndpointState;
import org.jboss.wsf.spi.invocation.Invocation;
import org.jboss.wsf.spi.security.SecurityDomainContext;
import org.wildfly.transaction.client.ContextTransactionManager;
import org.wildfly.transaction.client.LocalTransactionContext;

/**
 * Invocation abstraction for all endpoint types
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:ema@redhat.com">Jim Ma</a>
 */
abstract class AbstractInvocationHandler extends org.jboss.ws.common.invocation.AbstractInvocationHandler {

   private volatile ServiceName componentViewName;
   private volatile ComponentView componentView;
   protected volatile ManagedReference reference;

   /**
    * Initializes component view name.
    *
    * @param endpoint web service endpoint
    */
   public void init(final Endpoint endpoint) {
       componentViewName = (ServiceName) endpoint.getProperty(COMPONENT_VIEW_NAME);
   }

    /**
     * Gets endpoint container lazily.
     *
     * @return endpoint container
     */
    protected ComponentView getComponentView() {
        ComponentView cv = componentView;
        // we need to check both, otherwise it is possible for
        // componentView to be initialized before reference
        if (cv == null) {
            synchronized (this) {
                cv = componentView;
                if (cv == null) {
                    cv = getMSCService(componentViewName, ComponentView.class);
                    if (cv == null) {
                        throw WSLogger.ROOT_LOGGER.cannotFindComponentView(componentViewName);
                    }
                    if (reference == null) {
                        try {
                            reference = cv.createInstance();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                    componentView = cv;
                }
            }
        }
        return cv;
    }

   /**
    * Invokes WS endpoint.
    *
    * @param endpoint WS endpoint
    * @param wsInvocation web service invocation
    * @throws Exception if any error occurs
    */
    public void invoke(final Endpoint endpoint, final Invocation wsInvocation) throws Exception {
        try {
            if (!EndpointState.STARTED.equals(endpoint.getState())) {
                throw WSLogger.ROOT_LOGGER.endpointAlreadyStopped(endpoint.getShortName());
            }
            SecurityDomainContext securityDomainContext = endpoint.getSecurityDomainContext();
            securityDomainContext.runAs((Callable<Void>) () -> {
                invokeInternal(endpoint, wsInvocation);
                return null;
            });
        } catch (Throwable t) {
            handleInvocationException(t);
        } finally {
            onAfterInvocation(wsInvocation);
        }
    }

    public void invokeInternal(final Endpoint endpoint, final Invocation wsInvocation) throws Exception {
        // prepare for invocation
        onBeforeInvocation(wsInvocation);
        // prepare invocation data
        final ComponentView componentView = getComponentView();
        Component component = componentView.getComponent();
        // in case of @FactoryType annotation we don't need to go into EE interceptors
        final boolean forceTargetBean = (wsInvocation.getInvocationContext().getProperty("forceTargetBean") != null);
        if (forceTargetBean) {
            this.reference = new ManagedReference() {
                public void release() {
                }

                public Object getInstance() {
                    return wsInvocation.getInvocationContext().getTargetBean();
                }
            };
            if (component instanceof WSComponent) {
                ((WSComponent) component).setReference(reference);
            }
        }
        final Method method = getComponentViewMethod(wsInvocation.getJavaMethod(), componentView.getViewMethods());
        final InterceptorContext context = new InterceptorContext();
        prepareForInvocation(context, wsInvocation);
        context.setMethod(method);
        context.setParameters(wsInvocation.getArgs());
        context.putPrivateData(Component.class, component);
        context.putPrivateData(ComponentView.class, componentView);
        // pull in any XTS transaction
        LocalTransactionContext.getCurrent().importProviderTransaction();
        context.setTransaction(ContextTransactionManager.getInstance().getTransaction());
        if (forceTargetBean) {
            context.putPrivateData(ManagedReference.class, reference);
        }
        // invoke method
        final Object retObj = componentView.invoke(context);
        // set return value
        wsInvocation.setReturnValue(retObj);
    }

   protected void prepareForInvocation(final InterceptorContext context, final Invocation wsInvocation) {
      // does nothing
   }

   /**
    * Translates SEI method to component view method.
    *
    * @param seiMethod SEI method
    * @param viewMethods component view methods
    * @return matching component view method
    */
   protected Method getComponentViewMethod(final Method seiMethod, final Collection<Method> viewMethods) {
       for (final Method viewMethod : viewMethods) {
           if (matches(seiMethod, viewMethod)) {
               return viewMethod;
           }
       }
       throw new IllegalStateException();
   }

   protected void handleInvocationException(final Throwable t) throws Exception {
      if (t instanceof MBeanException) {
         throw ((MBeanException) t).getTargetException();
      }
      if (t instanceof Exception) {
         if (t instanceof InvocationTargetException) {
            throw (Exception) t;
         } else {
            SOAPFaultException ex = findSoapFaultException(t);
            if (ex != null) {
               throw new InvocationTargetException(ex);
            }
            throw new InvocationTargetException(t);
         }
      }
      if (t instanceof Error) {
         throw (Error) t;
      }
      throw new UndeclaredThrowableException(t);
    }

    protected SOAPFaultException findSoapFaultException(Throwable ex) {
        if (ex instanceof SOAPFaultException) {
            return (SOAPFaultException) ex;
        }
        if (ex.getCause() != null) {
            return findSoapFaultException(ex.getCause());
        }
        return null;
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

}

