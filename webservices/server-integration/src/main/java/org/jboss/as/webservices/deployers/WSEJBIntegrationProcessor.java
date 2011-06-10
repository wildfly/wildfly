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
package org.jboss.as.webservices.deployers;

import static org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION;
import static org.jboss.as.webservices.util.WSAttachmentKeys.WEBSERVICE_DEPLOYMENT_KEY;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.component.EJBViewDescription;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.webservices.util.ASHelper;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.wsf.spi.deployment.integration.WebServiceDeclaration;
import org.jboss.wsf.spi.deployment.integration.WebServiceDeployment;

/**
 * WebServiceDeployment deployer processes EJB containers and its metadata and creates WS adapters wrapping it.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class WSEJBIntegrationProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit unit = phaseContext.getDeploymentUnit();
        final WebServiceDeployment wsDeploymentAdapter = new WebServiceDeploymentAdapter();
        processAnnotation(unit, ASHelper.WEB_SERVICE_ANNOTATION, wsDeploymentAdapter);
        processAnnotation(unit, ASHelper.WEB_SERVICE_PROVIDER_ANNOTATION, wsDeploymentAdapter);
        if (!wsDeploymentAdapter.getServiceEndpoints().isEmpty()) {
            unit.putAttachment(WEBSERVICE_DEPLOYMENT_KEY, wsDeploymentAdapter);
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {
        // NOOP
    }

   private static void processAnnotation(final DeploymentUnit unit, final DotName annotation, final WebServiceDeployment wsDeployment) {
       final List<AnnotationInstance> webServiceAnnotations = ASHelper.getAnnotations(unit, annotation);
       final List<WebServiceDeclaration> endpoints = wsDeployment.getServiceEndpoints();
       final EEModuleDescription moduleDescription = unit.getAttachment(EE_MODULE_DESCRIPTION);

       for (final AnnotationInstance webServiceAnnotation : webServiceAnnotations) {
           final AnnotationTarget target = webServiceAnnotation.target();
           final ClassInfo webServiceClassInfo = (ClassInfo) target;
           final String beanClassName = webServiceClassInfo.name().toString();

           final List<ComponentDescription> componentDescriptions = moduleDescription.getComponentsByClassName(beanClassName);

           // final String componentName = beanClassName.substring(beanClassName.lastIndexOf(".") + 1); // TODO: investigate why commented out
           // final ServiceName baseName = unit.getServiceName().append("component").append(componentName).append("START"); // TODO: investigate why commented out
           final List<SessionBeanComponentDescription> sessionBeans = getSessionBeans(componentDescriptions);
           for(SessionBeanComponentDescription sessionBean : sessionBeans) {
               if (sessionBean.isStateless() || sessionBean.isSingleton()) {
                   final EJBViewDescription ejbViewDescription = sessionBean.addWebserviceEndpointView();
                   final String ejbViewName = ejbViewDescription.getServiceName().getCanonicalName();
                   endpoints.add(new WebServiceDeclarationAdapter(sessionBean, webServiceClassInfo, ejbViewName));
               }
           }
       }
   }

   private static List<SessionBeanComponentDescription> getSessionBeans(final List<ComponentDescription> componentDescriptions) {
       final List<SessionBeanComponentDescription> beans = new ArrayList<SessionBeanComponentDescription>(1);
       for(ComponentDescription componentDescription : componentDescriptions) {
           if (componentDescription instanceof SessionBeanComponentDescription) {
               beans.add((SessionBeanComponentDescription)componentDescription);
           }
       }
       return beans;
   }

   /**
    * Adopts EJB3 bean meta data to a
    * {@link org.jboss.wsf.spi.deployment.integration.WebServiceDeclaration}.
    */
   private static final class WebServiceDeclarationAdapter implements WebServiceDeclaration {

      /** EJB meta data. */
      private final SessionBeanComponentDescription ejbMD;
      private final ClassInfo webServiceClassInfo; // TODO: propagate just annotations?
      private final String containerName;

      /**
       * Constructor.
       *
       * @param ejbMD EJB metadata
       */
      private WebServiceDeclarationAdapter(final SessionBeanComponentDescription ejbMD, final ClassInfo webServiceClassInfo, final String containerName) {
         this.ejbMD = ejbMD;
         this.webServiceClassInfo = webServiceClassInfo;
         this.containerName = containerName;
      }

      /**
       * Returns EJB container name.
       *
       * @return container name
       */
      public String getContainerName() {
         return containerName;
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
      /** List of EJB endpoints. */
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
