/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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

import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Map;

//import javax.ejb.embeddable.EJBContainer; // TODO: needed?
import javax.naming.Context;
import javax.naming.NamingException;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.MessageContext;

import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ee.component.ViewService;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.ejb3.component.stateless.StatelessSessionComponent;
import org.jboss.as.webservices.util.ASHelper;
import org.jboss.msc.service.ServiceName;
import org.jboss.wsf.common.injection.ThreadLocalAwareWebServiceContext;
import org.jboss.wsf.common.invocation.AbstractInvocationHandler;
import org.jboss.wsf.spi.SPIProvider;
import org.jboss.wsf.spi.SPIProviderResolver;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.invocation.Invocation;
import org.jboss.wsf.spi.invocation.InvocationContext;
import org.jboss.wsf.spi.invocation.integration.InvocationContextCallback;
import org.jboss.wsf.spi.invocation.integration.ServiceEndpointContainer;
import org.jboss.wsf.spi.ioc.IoCContainerProxy;
import org.jboss.wsf.spi.ioc.IoCContainerProxyFactory;
import org.w3c.dom.Element;

/**
 * Handles invocations on EJB3 endpoints.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:tdiesler@redhat.com">Thomas Diesler</a>
 */
final class InvocationHandlerEJB3 extends AbstractInvocationHandler {
   /** EJB3 JNDI context. */
   private static final String EJB3_JNDI_PREFIX = "java:env/";

   /** MC kernel controller. */
   private final IoCContainerProxy iocContainer;

   /** EJB3 container name. */
   private String containerName;

   /** EJB3 container. */
   private StatelessSessionComponent serviceEndpointContainer;

   /**
    * Constructor.
    */
   InvocationHandlerEJB3() {
      final SPIProvider spiProvider = SPIProviderResolver.getInstance().getProvider();
      final IoCContainerProxyFactory iocContainerFactory = spiProvider.getSPI(IoCContainerProxyFactory.class);
      this.iocContainer = iocContainerFactory.getContainer();
   }

   /**
    * Initializes EJB3 container name.
    *
    * @param endpoint web service endpoint
    */
   public void init(final Endpoint endpoint) {
      this.containerName = (String) endpoint.getProperty(ASHelper.CONTAINER_NAME);

      if (this.containerName == null) {
         throw new IllegalArgumentException("Container name cannot be null");
      }
   }

   /**
    * Gets EJB 3 container lazily.
    *
    * @return EJB3 container
    */
   private synchronized StatelessSessionComponent getEjb3Container() {
      final boolean ejb3ContainerNotInitialized = this.serviceEndpointContainer == null;

      if (ejb3ContainerNotInitialized) {
         this.serviceEndpointContainer = this.iocContainer.getBean(this.containerName, StatelessSessionComponent.class);
         if (this.serviceEndpointContainer == null) {
            throw new WebServiceException("Cannot find service endpoint target: " + this.containerName);
         }
      }

      return this.serviceEndpointContainer;
   }

   private Object getEjb3Instance(final StatelessSessionComponent ssc, final Invocation wsInvocation) {
       Class<?> ifaceClazz = ssc.getComponentClass().getInterfaces()[0];
       return ssc.getComponentView(ifaceClazz).getReference().getInstance();
   }

   /**
    * Invokes EJB 3 endpoint.
    *
    * @param endpoint EJB 3 endpoint
    * @param wsInvocation web service invocation
    * @throws Exception if any error occurs
    */
   public void invoke(final Endpoint endpoint, final Invocation wsInvocation) throws Exception {
      try {
         // prepare for invocation
         this.onBeforeInvocation(wsInvocation);

         final StatelessSessionComponent ejbContainer = this.getEjb3Container();
         final Object instance = getEjb3Instance(ejbContainer, wsInvocation);

         final Class<?> implClass = instance.getClass();
         final Method seiMethod = wsInvocation.getJavaMethod();
         final Method implMethod = this.getImplMethod(implClass, seiMethod);
         final Object[] args = wsInvocation.getArgs();
         // invoke method
         final Object retObj = implMethod.invoke(instance, args); // TODO: there used to be ServiceEndpointContainer, how to do it in AS7?
         wsInvocation.setReturnValue(retObj);
         /*
         final ServiceEndpointContainer ejbContainer = this.getEjb3Container();
         final InvocationContextCallback invocationCallback = new EJB3InvocationContextCallback(wsInvocation);
         final Class<?> implClass = ejbContainer.getServiceImplementationClass();
         final Method seiMethod = wsInvocation.getJavaMethod();
         final Method implMethod = this.getImplMethod(implClass, seiMethod);
         final Object[] args = wsInvocation.getArgs();

         // invoke method
         final Object retObj = ejbContainer.invokeEndpoint(implMethod, args, invocationCallback);
         wsInvocation.setReturnValue(retObj);
         */
      }
      catch (Throwable t) {
         this.log.error("Method invocation failed with exception: " + t.getMessage(), t);
         this.handleInvocationException(t);
      }
      finally {
         this.onAfterInvocation(wsInvocation);
      }
   }

   public Context getJNDIContext(final Endpoint ep) throws NamingException {
      return null; // TODO: implement
//      final EJBContainer ejb3Container = (EJBContainer) getEjb3Container();
//      return (Context) ejb3Container.getEnc().lookup(EJB3_JNDI_PREFIX);
   }

   /**
    * Injects webservice context on target bean.
    *
    *  @param invocation current invocation
    */
   @Override
   public void onBeforeInvocation(final Invocation invocation) {
      final WebServiceContext wsContext = this.getWebServiceContext(invocation);
      ThreadLocalAwareWebServiceContext.getInstance().setMessageContext(wsContext);
   }

   /**
    * Cleanups injected webservice context on target bean.
    *
    * @param invocation current invocation
    */
   @Override
   public void onAfterInvocation(final Invocation invocation) {
      ThreadLocalAwareWebServiceContext.getInstance().setMessageContext(null);
   }

   /**
    * Returns WebServiceContext associated with this invocation.
    *
    * @param invocation current invocation
    * @return web service context or null if not available
    */
   private WebServiceContext getWebServiceContext(final Invocation invocation) {
      final InvocationContext invocationContext = invocation.getInvocationContext();

      return new WebServiceContextAdapter(invocationContext.getAttachment(WebServiceContext.class));
   }

   private static final class WebServiceContextAdapter implements WebServiceContext {
      private final WebServiceContext delegate;

      private WebServiceContextAdapter(final WebServiceContext delegate) {
         this.delegate = delegate;
      }

      public MessageContext getMessageContext() {
         return this.delegate.getMessageContext();
      }

      public Principal getUserPrincipal() {
         throw new UnsupportedOperationException();
         // return CurrentEJBContext.get().getCallerPrincipal();
      }

      public boolean isUserInRole(final String role) {
         throw new UnsupportedOperationException();
         //return CurrentEJBContext.get().isCallerInRole(role);
      }

      public EndpointReference getEndpointReference(final Element... referenceParameters) {
         return delegate.getEndpointReference(referenceParameters);
      }

      public <T extends EndpointReference> T getEndpointReference(final Class<T> clazz, final Element... referenceParameters) {
         return delegate.getEndpointReference(clazz, referenceParameters);
      }
   }

   /**
    * EJB3 invocation callback allowing EJB 3 beans to access Web Service invocation properties.
    */
   private static final class EJB3InvocationContextCallback implements InvocationContextCallback {
      /** WebService invocation. */
      private Invocation wsInvocation;

      /**
       * Constructor.
       *
       * @param wsInvocation delegee
       */
      public EJB3InvocationContextCallback(final Invocation wsInvocation) {
         this.wsInvocation = wsInvocation;
      }

      /**
       * Retrieves attachment type from Web Service invocation context attachments.
       *
       * @param <T> attachment type
       * @param attachmentType attachment class
       * @return attachment value
       */
      public <T> T get(final Class<T> attachmentType) {
         return this.wsInvocation.getInvocationContext().getAttachment(attachmentType);
      }
   }
}
