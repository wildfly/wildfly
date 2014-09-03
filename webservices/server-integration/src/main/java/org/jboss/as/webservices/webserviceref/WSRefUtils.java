/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.webservices.webserviceref;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceRef;
import javax.xml.ws.WebServiceRefs;
import javax.xml.ws.soap.MTOM;

import org.jboss.as.webservices.logging.WSLogger;
import org.jboss.metadata.javaee.jboss.JBossPortComponentRef;
import org.jboss.metadata.javaee.jboss.JBossServiceReferenceMetaData;
import org.jboss.metadata.javaee.jboss.StubPropertyMetaData;
import org.jboss.metadata.javaee.spec.Addressing;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.javaee.spec.PortComponentRef;
import org.jboss.metadata.javaee.spec.ServiceReferenceHandlerChainMetaData;
import org.jboss.metadata.javaee.spec.ServiceReferenceHandlerChainsMetaData;
import org.jboss.metadata.javaee.spec.ServiceReferenceHandlerMetaData;
import org.jboss.metadata.javaee.spec.ServiceReferenceMetaData;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.AddressingMetadata;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.MTOMMetadata;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.RespectBindingMetadata;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerChainMetaData;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerChainsMetaData;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerMetaData;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedInitParamMetaData;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedPortComponentRefMetaData;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedPortComponentRefMetaDataBuilder;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedServiceRefMetaData;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedServiceRefMetaDataBuilder;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedStubPropertyMetaData;

/**
 * Translates WS Refs from JBossAS MD to JBossWS UMDM format.
 *
 * Some of the methods on this class are public to allow weld
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class WSRefUtils {

    private WSRefUtils() {
    }

    static UnifiedServiceRefMetaData translate(final ServiceReferenceMetaData serviceRefMD) {
        UnifiedServiceRefMetaDataBuilder builder = new UnifiedServiceRefMetaDataBuilder();
        builder.setServiceRefName(serviceRefMD.getName());
        builder.setServiceRefType(serviceRefMD.getServiceRefType());
        builder.setServiceInterface(serviceRefMD.getServiceInterface());
        builder.setWsdlFile(serviceRefMD.getWsdlFile());
        builder.setMappingFile(serviceRefMD.getJaxrpcMappingFile());
        builder.setServiceQName(serviceRefMD.getServiceQname());

        // propagate port components
        final Collection<? extends PortComponentRef> portComponentsMD = serviceRefMD.getPortComponentRef();
        if (portComponentsMD != null) {
            for (final PortComponentRef portComponentMD : portComponentsMD) {
                final UnifiedPortComponentRefMetaData portComponentUMDM = getUnifiedPortComponentRefMetaData(portComponentMD);
                if (portComponentUMDM.getServiceEndpointInterface() != null || portComponentUMDM.getPortQName() != null) {
                    builder.addPortComponentRef(portComponentUMDM);
                } else {
                    WSLogger.ROOT_LOGGER.ignoringPortComponentRef(portComponentUMDM);
                }
            }
        }

        // propagate handlers
        final Collection<ServiceReferenceHandlerMetaData> handlersMD = serviceRefMD.getHandlers();
        if (handlersMD != null) {
           for (final ServiceReferenceHandlerMetaData handlerMD : handlersMD) {
              final UnifiedHandlerMetaData handlerUMDM = getUnifiedHandlerMetaData(handlerMD);
              builder.addHandler(handlerUMDM);
           }
        }

        // propagate handler chains
        ServiceReferenceHandlerChainsMetaData handlerChainsMD = serviceRefMD.getHandlerChains();
        if (handlerChainsMD != null) {
           final UnifiedHandlerChainsMetaData handlerChainsUMDM = getUnifiedHandlerChainsMetaData(handlerChainsMD);
           builder.setHandlerChains(handlerChainsUMDM);
        }

        // propagate jboss specific MD
        if (serviceRefMD instanceof JBossServiceReferenceMetaData) {
           final JBossServiceReferenceMetaData jbossServiceRefMD = (JBossServiceReferenceMetaData) serviceRefMD;
           builder.setServiceImplClass(jbossServiceRefMD.getServiceClass());
           builder.setConfigName(jbossServiceRefMD.getConfigName());
           builder.setConfigFile(jbossServiceRefMD.getConfigFile());
           builder.setWsdlOverride(jbossServiceRefMD.getWsdlOverride());
           builder.setHandlerChain(jbossServiceRefMD.getHandlerChain());
        }

        return builder.build();
    }

    private static UnifiedPortComponentRefMetaData getUnifiedPortComponentRefMetaData(final PortComponentRef portComponentMD) {
        final UnifiedPortComponentRefMetaDataBuilder builder = new UnifiedPortComponentRefMetaDataBuilder();

        // propagate service endpoint interface
        builder.setServiceEndpointInterface(portComponentMD.getServiceEndpointInterface());

        // propagate MTOM properties
        builder.setMtomEnabled(portComponentMD.isEnableMtom());
        builder.setMtomThreshold(portComponentMD.getMtomThreshold());

        // propagate addressing properties
        final Addressing addressingMD = portComponentMD.getAddressing();
        if (addressingMD != null) {
            builder.setAddressingAnnotationSpecified(true);
            builder.setAddressingEnabled(addressingMD.isEnabled());
            builder.setAddressingRequired(addressingMD.isRequired());
            builder.setAddressingResponses(addressingMD.getResponses());
        }

        // propagate respect binding properties
        if (portComponentMD.getRespectBinding() != null) {
            builder.setRespectBindingAnnotationSpecified(true);
            builder.setRespectBindingEnabled(true);
        }

        // propagate link
        builder.setPortComponentLink(portComponentMD.getPortComponentLink());

        // propagate jboss specific MD
        if (portComponentMD instanceof JBossPortComponentRef) {
            final JBossPortComponentRef jbossPortComponentMD = (JBossPortComponentRef) portComponentMD;

            // propagate port QName
            builder.setPortQName(jbossPortComponentMD.getPortQname());

            // propagate configuration properties
            builder.setConfigName(jbossPortComponentMD.getConfigName());
            builder.setConfigFile(jbossPortComponentMD.getConfigFile());

            // propagate stub properties
            final List<StubPropertyMetaData> stubPropertiesMD = jbossPortComponentMD.getStubProperties();
            if (stubPropertiesMD != null) {
                for (final StubPropertyMetaData stubPropertyMD : stubPropertiesMD) {
                    builder.addStubProperty(new UnifiedStubPropertyMetaData(stubPropertyMD.getPropName(), stubPropertyMD.getPropValue()));
                }
            }
        }

        return builder.build();
    }

    private static UnifiedHandlerMetaData getUnifiedHandlerMetaData(ServiceReferenceHandlerMetaData srhmd) {
        List<UnifiedInitParamMetaData> unifiedInitParamMDs = new LinkedList<UnifiedInitParamMetaData>();
        List<ParamValueMetaData> initParams = srhmd.getInitParam();
        if (initParams != null) {
            for (ParamValueMetaData initParam : initParams) {
                unifiedInitParamMDs.add(new UnifiedInitParamMetaData(initParam.getParamName(), initParam.getParamValue()));
            }
        }
        List<QName> soapHeaders = srhmd.getSoapHeader();
        Set<QName> soapHeaderList = soapHeaders != null ? new HashSet<QName>(soapHeaders) : null;
        List<String> soapRoles = srhmd.getSoapRole();
        Set<String> soapRolesList = soapRoles != null ? new HashSet<String>(soapRoles) : null;
        List<String> portNames = srhmd.getPortName();
        Set<String> portNameList = portNames != null ? new HashSet<String>(portNames) : null;
        return new UnifiedHandlerMetaData(srhmd.getHandlerClass(), srhmd.getHandlerName(), unifiedInitParamMDs, soapHeaderList, soapRolesList, portNameList);
    }

    private static UnifiedHandlerChainsMetaData getUnifiedHandlerChainsMetaData(final ServiceReferenceHandlerChainsMetaData handlerChainsMD) {
        List<UnifiedHandlerChainMetaData> uhcmds = new LinkedList<UnifiedHandlerChainMetaData>();
        for (final ServiceReferenceHandlerChainMetaData handlerChainMD : handlerChainsMD.getHandlers()) {
            List<UnifiedHandlerMetaData> uhmds = new LinkedList<UnifiedHandlerMetaData>();
            for (final ServiceReferenceHandlerMetaData handlerMD : handlerChainMD.getHandler()) {
                final UnifiedHandlerMetaData handlerUMDM = getUnifiedHandlerMetaData(handlerMD);
                uhmds.add(handlerUMDM);
            }
            uhcmds.add(new UnifiedHandlerChainMetaData(handlerChainMD.getServiceNamePattern(), handlerChainMD.getPortNamePattern(),
                    handlerChainMD.getProtocolBindings(), uhmds, false, null));
        }

        return new UnifiedHandlerChainsMetaData(uhcmds);
    }

    static void processAnnotatedElement(final AnnotatedElement anElement, final UnifiedServiceRefMetaData serviceRefUMDM) {
       processAddressingAnnotation(anElement, serviceRefUMDM);
       processMTOMAnnotation(anElement, serviceRefUMDM);
       processRespectBindingAnnotation(anElement, serviceRefUMDM);
       processHandlerChainAnnotation(anElement, serviceRefUMDM);
       processServiceRefType(anElement, serviceRefUMDM);
    }

    private static void processAddressingAnnotation(final AnnotatedElement anElement, final UnifiedServiceRefMetaData serviceRefUMDM) {
         final javax.xml.ws.soap.Addressing addressingAnnotation = getAnnotation(anElement, javax.xml.ws.soap.Addressing.class);

         if (addressingAnnotation != null) {
             serviceRefUMDM.setAddressingMedadata(new AddressingMetadata(true,
                    addressingAnnotation.enabled(),
                    addressingAnnotation.required(),
                    addressingAnnotation.responses().toString()));
         }
      }

      private static void processMTOMAnnotation(final AnnotatedElement anElement, final UnifiedServiceRefMetaData serviceRefUMDM) {
         final MTOM mtomAnnotation = getAnnotation(anElement, MTOM.class);

         if (mtomAnnotation != null) {
            serviceRefUMDM.setMTOMMetadata(new MTOMMetadata(true, mtomAnnotation.enabled(), mtomAnnotation.threshold()));
         }
      }

      private static void processRespectBindingAnnotation(final AnnotatedElement anElement, final UnifiedServiceRefMetaData serviceRefUMDM) {
         final javax.xml.ws.RespectBinding respectBindingAnnotation = getAnnotation(anElement, javax.xml.ws.RespectBinding.class);

         if (respectBindingAnnotation != null) {
            serviceRefUMDM.setRespectBindingMetadata(new RespectBindingMetadata(true, respectBindingAnnotation.enabled()));
         }
      }

      private static  void processServiceRefType(final AnnotatedElement anElement, final UnifiedServiceRefMetaData serviceRefUMDM) {
         if (anElement instanceof Field) {
            final Class<?> targetClass = ((Field) anElement).getType();
            serviceRefUMDM.setServiceRefType(targetClass.getName());

            if (Service.class.isAssignableFrom(targetClass))
               serviceRefUMDM.setServiceInterface(targetClass.getName());
         } else if (anElement instanceof Method) {
            final Class<?> targetClass = ((Method) anElement).getParameterTypes()[0];
            serviceRefUMDM.setServiceRefType(targetClass.getName());

            if (Service.class.isAssignableFrom(targetClass))
               serviceRefUMDM.setServiceInterface(targetClass.getName());
         } else {
            final WebServiceRef serviceRefAnnotation = getWebServiceRefAnnotation(anElement, serviceRefUMDM);
            Class<?> targetClass = null;
            if (serviceRefAnnotation != null && (serviceRefAnnotation.type() != Object.class))
            {
               targetClass = serviceRefAnnotation.type();
               serviceRefUMDM.setServiceRefType(targetClass.getName());

               if (Service.class.isAssignableFrom(targetClass))
                  serviceRefUMDM.setServiceInterface(targetClass.getName());
            }
         }
      }

      private static void processHandlerChainAnnotation(final AnnotatedElement anElement, final UnifiedServiceRefMetaData serviceRefUMDM) {
         final javax.jws.HandlerChain handlerChainAnnotation = getAnnotation(anElement, javax.jws.HandlerChain.class);

         if (handlerChainAnnotation != null) {
            // Set the handlerChain from @HandlerChain on the annotated element
            String handlerChain = null;
            if (handlerChainAnnotation.file().length() > 0)
               handlerChain = handlerChainAnnotation.file();

            // Resolve path to handler chain
            if (handlerChain != null) {
               try {
                  new URL(handlerChain);
               } catch (MalformedURLException ignored) {
                  final Class<?> declaringClass = getDeclaringClass(anElement);

                  handlerChain = declaringClass.getPackage().getName().replace('.', '/') + "/" + handlerChain;
               }

               serviceRefUMDM.setHandlerChain(handlerChain);
            }
         }
      }

      private static <T extends Annotation> T getAnnotation(final AnnotatedElement anElement, final Class<T> annotationClass) {
         return anElement != null ? (T) anElement.getAnnotation(annotationClass) : null;
      }

      private static Class<?> getDeclaringClass(final AnnotatedElement annotatedElement) {
         Class<?> declaringClass = null;
         if (annotatedElement instanceof Field) {
            declaringClass = ((Field) annotatedElement).getDeclaringClass();
         } else if (annotatedElement instanceof Method) {
            declaringClass = ((Method) annotatedElement).getDeclaringClass();
         } else if (annotatedElement instanceof Class) {
            declaringClass = (Class<?>) annotatedElement;
         }

         return declaringClass;
      }

      private static WebServiceRef getWebServiceRefAnnotation(final AnnotatedElement anElement, final UnifiedServiceRefMetaData serviceRefUMDM) {
          final WebServiceRef webServiceRefAnnotation = getAnnotation(anElement, WebServiceRef.class);
          final WebServiceRefs webServiceRefsAnnotation = getAnnotation(anElement, WebServiceRefs.class);

          if (webServiceRefAnnotation == null && webServiceRefsAnnotation == null) {
              return null;
          }

          // Build the list of @WebServiceRef relevant annotations
          final List<WebServiceRef> wsrefList = new ArrayList<WebServiceRef>();

          if (webServiceRefAnnotation != null) {
              wsrefList.add(webServiceRefAnnotation);
          }

          if (webServiceRefsAnnotation != null) {
              for (final WebServiceRef webServiceRefAnn : webServiceRefsAnnotation.value()) {
                  wsrefList.add(webServiceRefAnn);
              }
          }

          // Return effective @WebServiceRef annotation
          WebServiceRef returnValue = null;
          if (wsrefList.size() == 1) {
              returnValue = wsrefList.get(0);
          } else {
              for (WebServiceRef webServiceRefAnn : wsrefList) {
                  if (serviceRefUMDM.getServiceRefName().endsWith(webServiceRefAnn.name())) {
                      returnValue = webServiceRefAnn;
                      break;
                  }
              }
          }

          return returnValue;
      }

}
