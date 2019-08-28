/*
 * Copyright (C) 2019 Red Hat, inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.jboss.as.jaxrs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.global.ReadAttributeHandler;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.jaxrs.logging.JaxrsLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

public class JaxrsContextParamHandler {

   // Attributes in alphabetical order
   public static final String JAX_RS_2_0_REQUEST_MATCHING = "jaxrs.2.0.request.matching";
   public static final String RESTEASY_ADD_CHARSET = "resteasy.add.charset";
   public static final String RESTEASY_BUFFER_EXCEPTION_ENTITY = "resteasy.buffer.exception.entity";
   public static final String RESTEASY_DISABLE_DTDS = "resteasy.document.secure.disableDTDs";
   public static final String RESTEASY_DISABLE_HTML_SANITIZER = "resteasy.disable.html.sanitizer";
   public static final String RESTEASY_DISABLE_PROVIDERS = "resteasy.disable.providers";
   public static final String RESTEASY_EXPAND_ENTITY_REFERENCES = "resteasy.document.expand.entity.references";
   public static final String RESTEASY_GZIP_MAX_INPUT = "resteasy.gzip.max.input";
   public static final String RESTEASY_JNDI_RESOURCES = "resteasy.jndi.resources";
   public static final String RESTEASY_LANGUAGE_MAPPINGS = "resteasy.language.mappings";
   public static final String RESTEASY_MEDIA_TYPE_MAPPINGS = "resteasy.media.type.mappings";
   public static final String RESTEASY_MEDIA_TYPE_PARAM_MAPPING = "resteasy.media.type.param.mapping";
   public static final String RESTEASY_PROVIDERS = "resteasy.providers";
   public static final String RESTEASY_RESOURCES = "resteasy.resources";
   public static final String RESTEASY_RFC7232_PRECONDITIONS = "resteasy.rfc7232preconditions";
   public static final String RESTEASY_ROLE_BASED_SECURITY = "resteasy.role.based.security";
   public static final String RESTEASY_SECURE_PROCESSING_FEATURE = "resteasy.document.secure.processing.feature";
   public static final String RESTEASY_SECURE_RANDOM_MAX_USE = "resteasy.secure.random.max.use";
   public static final String RESTEASY_USE_BUILTIN_PROVIDERS = "resteasy.use.builtin.providers";
   public static final String RESTEASY_USE_CONTAINER_FORM_PARAMS = "resteasy.use.container.form.params";
   public static final String RESTEASY_WIDER_REQUEST_MATCHING = "resteasy.wider.request.matching";

   private static final String JAVA_ID = "[a-zA-Z_$][a-zA-Z_$0-9]*(\\.[a-zA-Z_$][a-zA-Z_$0-9]*)*";
   private static final String JNDI_ID = JAVA_ID + ":" + JAVA_ID + "(/" + JAVA_ID + ")*";
   private static final Pattern PATTERN_LIST_ELEMENT = Pattern.compile(JAVA_ID + "|" + JNDI_ID);
   private static final Pattern PATTERN_PROPERTY_LIST_ELEMENT = Pattern.compile(".+:.+(,.+:.+)*");
   private static final String NO_DEFAULT = "noDefault";

   private static final Map<String, ModelNode> contextParams = new TreeMap<String, ModelNode>();

   private static final Map<String, Boolean> contextParametersBoolean = new HashMap<String, Boolean>();
   private static final Map<String, Integer> contextParametersInteger = new HashMap<String, Integer>();
   private static final Map<String, String>  contextParametersString = new HashMap<String, String>();
   private static final Set<String>          contextParametersList = new HashSet<String>();
   private static final Set<String>          contextParametersMap = new HashSet<String>();

   static {
      // boolean attributes
      contextParametersBoolean.put(JAX_RS_2_0_REQUEST_MATCHING, false);
      contextParametersBoolean.put(RESTEASY_ADD_CHARSET, true);
      contextParametersBoolean.put(RESTEASY_BUFFER_EXCEPTION_ENTITY, true);
      contextParametersBoolean.put(RESTEASY_EXPAND_ENTITY_REFERENCES, false);
      contextParametersBoolean.put(RESTEASY_DISABLE_DTDS, true);
      contextParametersBoolean.put(RESTEASY_DISABLE_HTML_SANITIZER, false);
      contextParametersBoolean.put(RESTEASY_RFC7232_PRECONDITIONS, false);
      contextParametersBoolean.put(RESTEASY_ROLE_BASED_SECURITY, false);
      contextParametersBoolean.put(RESTEASY_SECURE_PROCESSING_FEATURE, true);
      contextParametersBoolean.put(RESTEASY_USE_BUILTIN_PROVIDERS, true);
      contextParametersBoolean.put(RESTEASY_USE_CONTAINER_FORM_PARAMS, false);
      contextParametersBoolean.put(RESTEASY_WIDER_REQUEST_MATCHING, false);

      // integer attributes
      contextParametersInteger.put(RESTEASY_GZIP_MAX_INPUT, 10000000);
      contextParametersInteger.put(RESTEASY_SECURE_RANDOM_MAX_USE, 100);

      // string attributes
      contextParametersString.put(RESTEASY_MEDIA_TYPE_PARAM_MAPPING, NO_DEFAULT);

      // list attributes
      contextParametersList.add(RESTEASY_DISABLE_PROVIDERS);
      contextParametersList.add(RESTEASY_JNDI_RESOURCES);
      contextParametersList.add(RESTEASY_PROVIDERS);
      contextParametersList.add(RESTEASY_RESOURCES);

      // map attributes
      contextParametersMap.add(RESTEASY_LANGUAGE_MAPPINGS);
      contextParametersMap.add(RESTEASY_MEDIA_TYPE_MAPPINGS);
   }

   static final ResourceDescriptionResolver DEFAULT_RESOLVER = JaxrsExtension.getResolver("context-params");

   ///////////////////////////////////////////////////////////////////////////////
   ///                          Public functions                               ///
   ///////////////////////////////////////////////////////////////////////////////
   public static Map<String, ModelNode> getContextParameters() {
      return contextParams;
   }

   public static ModelNode getContextParam(String name) {
      ModelNode modelNode = contextParams.get(name);
      if (modelNode == null) {
         return new ModelNode();
      } else {
         return modelNode;
      }
   }

   public static void setContextParam(String name, ModelNode value) {
      contextParams.put(name, value);
   }

   public static Map<String, Boolean> getContextParametersBoolean() {
      return contextParametersBoolean;
   }

   public static Map<String, Integer> getContextParametersInteger() {
      return contextParametersInteger;
   }

   public static Map<String, String> getContextParametersString() {
      return contextParametersString;
   }

   public static Set<String> getContextParametersList() {
      return contextParametersList;
   }

   public static Set<String> getContextParametersMap() {
      return contextParametersMap;
   }

   public static void registerAttributes(ManagementResourceRegistration resourceRegistration) {
      for (Entry<String, Boolean> entry : contextParametersBoolean.entrySet()) {
         registerAttribute(resourceRegistration, getAttributeDefinition(entry.getKey(), ModelType.BOOLEAN, new ModelNode(entry.getValue())));
      }
      for (Entry<String, Integer> entry : contextParametersInteger.entrySet()) {
         registerAttribute(resourceRegistration, getAttributeDefinition(entry.getKey(), ModelType.INT, new ModelNode(entry.getValue())));
      }
      for (Entry<String, String> entry : contextParametersString.entrySet()) {
         registerAttribute(resourceRegistration, getAttributeDefinition(entry.getKey(), ModelType.STRING, new ModelNode(entry.getValue())));
      }
      for (String name : contextParametersList) {
         registerAttribute(resourceRegistration, getListAttributeDefinition(name, ModelType.LIST));
      }
      for (String name : contextParametersMap) {
         registerAttribute(resourceRegistration, getMapAttributeDefinition(name, ModelType.LIST));
      }
   }

   ///////////////////////////////////////////////////////////////////////////////
   ///                          Private functions                              ///
   ///////////////////////////////////////////////////////////////////////////////
   private static AttributeDefinition getAttributeDefinition(String name, ModelType modelType, ModelNode defaultValue) {
      if (NO_DEFAULT.equals(defaultValue.asString())) {
         return new SimpleAttributeDefinitionBuilder(name, modelType)
               .setRequired(false)
               .setAllowExpression(true)
               .build();
      } else {
         return new SimpleAttributeDefinitionBuilder(name, modelType)
               .setRequired(false)
               .setAllowExpression(true)
               .setDefaultValue(defaultValue)
               .build();
      }
   }

   private static AttributeDefinition getListAttributeDefinition(String name, ModelType modelType) {
      return new StringListAttributeDefinition.Builder(name)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(JavaIdListValidator.INSTANCE)
            .build();
   }

   private static AttributeDefinition getMapAttributeDefinition(String name, ModelType modelType) {
      return new StringListAttributeDefinition.Builder(name)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(JavaMapValidator.INSTANCE)
            .build();
   }

   private static void registerAttribute(ManagementResourceRegistration resourceRegistration, AttributeDefinition attributeDefinition) {
      resourceRegistration.registerReadWriteAttribute(attributeDefinition, new ContextParamReadHandler(), new ContextParamWriteHandler(attributeDefinition));
   }

   ///////////////////////////////////////////////////////////////////////////////
   ///                               Handler Classes                           ///
   ///////////////////////////////////////////////////////////////////////////////
   static class ContextParamWriteHandler extends AbstractWriteAttributeHandler<Void> {

      private AttributeDefinition attributeDefinition;

      private ContextParamWriteHandler(AttributeDefinition attributeDefinition) {
         super(attributeDefinition);
         this.attributeDefinition = attributeDefinition;
      }

      @Override
      protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
            ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> handbackHolder) throws OperationFailedException {
         JaxrsContextParamHandler.setContextParam(attributeName, resolvedValue);
         return false;
      }

      @Override
      protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException{
         JaxrsContextParamHandler.setContextParam(attributeName, valueToRestore);
      }
   }

   static class ContextParamReadHandler extends ReadAttributeHandler {

       @Override
       public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
           context.getResult().set(JaxrsContextParamHandler.getContextParam(operation.get("name").asString()));
       }
   }

   static class JavaIdListValidator implements ParameterValidator {
      static JavaIdListValidator INSTANCE = new JavaIdListValidator();

      @Override
      public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
         if (PATTERN_LIST_ELEMENT.matcher(value.asString()).matches()) {
            return;
         }
         throw new OperationFailedException(JaxrsLogger.JAXRS_LOGGER.illegalArgument(parameterName, value.asString()));
      }
   }

   static class JavaMapValidator implements ParameterValidator {
      static JavaMapValidator INSTANCE = new JavaMapValidator();

      @Override
      public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
         if (PATTERN_PROPERTY_LIST_ELEMENT.matcher(value.asString()).matches()) {
            return;
         }
         throw new OperationFailedException(JaxrsLogger.JAXRS_LOGGER.illegalArgument(parameterName, value.asString()));
      }
   }
}
