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
package org.jboss.as.webservices.deployers;

import static org.jboss.as.webservices.util.WSAttachmentKeys.WEBSERVICE_DEPLOYMENT_KEY;
import static org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import javax.jws.WebService;
import javax.xml.ws.WebServiceProvider;

import org.jboss.as.ee.component.AbstractComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.ejb3.component.singleton.SingletonComponentDescription;
import org.jboss.as.ejb3.component.stateless.StatelessComponentDescription;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentException;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.webservices.util.ASHelper;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.logging.Logger;
import org.jboss.wsf.spi.deployment.integration.WebServiceDeclaration;
import org.jboss.wsf.spi.deployment.integration.WebServiceDeployment;

/**
 * WebServiceDeployment deployer processes EJB containers and its metadata and creates WS adapters wrapping it.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class WSEJBAdapterDeployer {

    private static final Logger LOGGER = Logger.getLogger(WSEJBAdapterDeployer.class);

   /**
    * Deploys WebServiceDeployment meta data.
    *
    * @param unit deployment unit
    * @throws DeploymentException exception
    */
   public static void internalDeploy(final DeploymentUnit unit) {
       final WebServiceDeploymentAdapter wsDeploymentAdapter = new WebServiceDeploymentAdapter();
       processAnnotation(unit, WebService.class, wsDeploymentAdapter);
       processAnnotation(unit, WebServiceProvider.class, wsDeploymentAdapter);
       unit.putAttachment(WEBSERVICE_DEPLOYMENT_KEY, wsDeploymentAdapter);
   }

   private static void processAnnotation(final DeploymentUnit unit, final Class<?> annClass, final WebServiceDeploymentAdapter wsDeploymentAdapter) {
       final List<AnnotationInstance> webServiceAnnotations = getAnnotations(unit, annClass);
       final List<WebServiceDeclaration> endpoints = wsDeploymentAdapter.getServiceEndpoints();
       if (webServiceAnnotations != null && !webServiceAnnotations.isEmpty()) {
           final EEModuleDescription moduleDescription = unit.getAttachment(EE_MODULE_DESCRIPTION);
           for (AnnotationInstance webServiceAnnotation : webServiceAnnotations) {
               final AnnotationTarget target = webServiceAnnotation.target();
               final ClassInfo webServiceClassInfo = (ClassInfo) target;
               final String beanClassName = webServiceClassInfo.name().toString();
               AbstractComponentDescription absCD = moduleDescription.getComponentByClassName(beanClassName);
               if (absCD instanceof StatelessComponentDescription || absCD instanceof SingletonComponentDescription) {
                   endpoints.add(new WebServiceDeclarationAdapter((SessionBeanComponentDescription)absCD, webServiceClassInfo));
               }
           }
       }
   }
   private static List<AnnotationInstance> getAnnotations(final DeploymentUnit unit, final Class<?> annClass) {
       final Index compositeIndex = ASHelper.getRootAnnotationIndex(unit);
       return compositeIndex.getAnnotations(DotName.createSimple(annClass.getName()));
   }

   /**
    * Adopts EJB3 bean meta data to a
    * {@link org.jboss.wsf.spi.deployment.integration.WebServiceDeclaration}.
    */
   private static final class WebServiceDeclarationAdapter implements WebServiceDeclaration {

      /** EJB meta data. */
      private final SessionBeanComponentDescription ejbMD;
      private final ClassInfo webServiceClassInfo; // TODO: propagate just annotations?

      /**
       * Constructor.
       *
       * @param ejbMetaData EJB metadata
       * @param ejbContainer EJB container
       * @param loader class loader
       */
      private WebServiceDeclarationAdapter(final SessionBeanComponentDescription ejbMD, final ClassInfo webServiceClassInfo/*, final ClassLoader loader*/) {
         this.ejbMD = ejbMD;
         this.webServiceClassInfo = webServiceClassInfo;
      }

      /**
       * Returns EJB container name.
       *
       * @return container name
       */
      public String getContainerName() {
         return "TODO: implement me " + this.getClass().getName(); // TODO: implement
      }

      /**
       * Returns EJB name.
       *
       * @return name
       */
      public String getComponentName() {
          return ejbMD.getComponentName();
      }

      /**
       * Returns EJB class name.
       *
       * @return class name
       */
      public String getComponentClassName() {
          return ejbMD.getComponentClassName();
      }

      /**
       * Returns requested annotation associated with EJB container or EJB bean.
       *
       * @param annotationType annotation type
       * @param <T> annotation class type
       * @return requested annotation or null if not found
       */
      public <T extends Annotation> T getAnnotation(final Class<T> annotationType) {// DotName
          throw new UnsupportedOperationException(); // TODO: implement
//         final boolean haveEjbContainer = this.ejbContainer != null;
//
//         if (haveEjbContainer)
//         {
//            return this.ejbContainer.getAnnotation(annotationType);
//         }
//         else
//         {
//            final Class<?> bean = this.getComponentClass();
//            return (T) bean.getAnnotation(annotationType);
//         }
      }

   }

   /**
    * Adopts an EJB deployment to a
    * {@link org.jboss.wsf.spi.deployment.integration.WebServiceDeployment}.
    */
   private static final class WebServiceDeploymentAdapter implements WebServiceDeployment {
      /** List of endpoints. */
      private final List<WebServiceDeclaration> endpoints = new ArrayList<WebServiceDeclaration>();

      /**
       * Constructor.
       */
      private WebServiceDeploymentAdapter() {
         super();
      }

      /**
       * Returns endpoints list.
       *
       * @return endpoints list
       */
      public List<WebServiceDeclaration> getServiceEndpoints() {
         return this.endpoints;
      }
   }
}