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

  package org.jboss.as.controller;

  import java.util.List;
  import java.util.Locale;
  import java.util.ResourceBundle;
  import javax.xml.stream.XMLStreamException;
  import javax.xml.stream.XMLStreamWriter;

  import org.jboss.as.controller.client.helpers.MeasurementUnit;
  import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
  import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
  import org.jboss.as.controller.operations.validation.AllowedValuesValidator;
  import org.jboss.as.controller.operations.validation.MinMaxValidator;
  import org.jboss.as.controller.operations.validation.ParameterValidator;
  import org.jboss.as.controller.registry.AttributeAccess;
  import org.jboss.dmr.ModelNode;
  import org.jboss.dmr.ModelType;

  /**
   * AttributeDefinition suitable for managing LISTs of OBJECTs, which takes into account
   * recursive processing of allowed values and their value types.
   *
   * Date: 13.10.2011
   *
   * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
   * @author Richard Achmatowicz (c) 2012 RedHat Inc.
   * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
   */
  public class ObjectListAttributeDefinition extends ListAttributeDefinition {
      private final ObjectTypeAttributeDefinition valueType;

      private ObjectListAttributeDefinition(final String name, final String xmlName, final ObjectTypeAttributeDefinition valueType, final boolean allowNull, final int minSize, final int maxSize, final String[] alternatives, final String[] requires, final AttributeAccess.Flag... flags) {
          super(name, xmlName, allowNull, minSize, maxSize, valueType.getValidator(), alternatives, requires, null,flags);
          this.valueType = valueType;
      }

      private ObjectListAttributeDefinition(final String name, final String xmlName, final ObjectTypeAttributeDefinition valueType, final boolean allowNull, final int minSize, final int maxSize, final String[] alternatives, final String[] requires, final AttributeMarshaller attributeMarshaller, final AttributeAccess.Flag... flags) {
          super(name, xmlName, allowNull, minSize, maxSize, valueType.getValidator(), alternatives, requires,attributeMarshaller, flags);
          this.valueType = valueType;
      }

      @Override
      public ModelNode addResourceAttributeDescription(ResourceBundle bundle, String prefix, ModelNode resourceDescription) {
          final ModelNode attr = getNoTextDescription(false);
          attr.get(ModelDescriptionConstants.DESCRIPTION).set(getAttributeTextDescription(bundle, prefix));
          final ModelNode result = resourceDescription.get(ModelDescriptionConstants.ATTRIBUTES, getName()).set(attr);
          addValueTypeDescription(result, prefix, bundle, false);
          return result;
      }

      @Override
      public ModelNode addOperationParameterDescription(ResourceBundle bundle, String prefix, ModelNode operationDescription) {
          final ModelNode param = getNoTextDescription(true);
          param.get(ModelDescriptionConstants.DESCRIPTION).set(getAttributeTextDescription(bundle, prefix));
          final ModelNode result = operationDescription.get(ModelDescriptionConstants.REQUEST_PROPERTIES, getName()).set(param);
          addValueTypeDescription(result, prefix, bundle, true);
          return result;
      }


      @Override
      protected void addValueTypeDescription(final ModelNode node, final ResourceBundle bundle) {
          addValueTypeDescription(node, valueType.getName(), bundle, false);
      }

      @Override
      protected void addAttributeValueTypeDescription(final ModelNode node, final ResourceDescriptionResolver resolver, final Locale locale, final ResourceBundle bundle) {
          addValueTypeDescription(node, getName(), bundle, false);
      }

      @Override
      protected void addOperationParameterValueTypeDescription(final ModelNode node, final String operationName, final ResourceDescriptionResolver resolver, final Locale locale, final ResourceBundle bundle) {
          addValueTypeDescription(node, getName(), bundle, true);
      }

      @Override
      public void marshallAsElement(final ModelNode resourceModel, final boolean marshalDefault, final XMLStreamWriter writer) throws XMLStreamException {
          if (resourceModel.hasDefined(getName())) {
              writer.writeStartElement(getXmlName());
              for (ModelNode handler : resourceModel.get(getName()).asList()) {
                  valueType.marshallAsElement(handler, writer);
              }
              writer.writeEndElement();
          }
      }

      protected void addValueTypeDescription(final ModelNode node, final String prefix, final ResourceBundle bundle, boolean forOperation) {
          node.get(ModelDescriptionConstants.DESCRIPTION); // placeholder
          node.get(ModelDescriptionConstants.EXPRESSIONS_ALLOWED).set(valueType.isAllowExpression());
          if (forOperation) {
              node.get(ModelDescriptionConstants.REQUIRED).set(!valueType.isAllowNull());
          }
          node.get(ModelDescriptionConstants.NILLABLE).set(isAllowNull());
          final ModelNode defaultValue = valueType.getDefaultValue();
          if (!forOperation && defaultValue != null && defaultValue.isDefined()) {
              node.get(ModelDescriptionConstants.DEFAULT).set(defaultValue);
          }
          MeasurementUnit measurementUnit = valueType.getMeasurementUnit();
          if (measurementUnit != null && measurementUnit != MeasurementUnit.NONE) {
              node.get(ModelDescriptionConstants.UNIT).set(measurementUnit.getName());
          }
          final String[] alternatives = valueType.getAlternatives();
          if (alternatives != null) {
              for (final String alternative : alternatives) {
                  node.get(ModelDescriptionConstants.ALTERNATIVES).add(alternative);
              }
          }
          final String[] requires = valueType.getRequires();
          if (requires != null) {
              for (final String required : requires) {
                  node.get(ModelDescriptionConstants.REQUIRES).add(required);
              }
          }
          final ParameterValidator validator = valueType.getValidator();
          if (validator instanceof MinMaxValidator) {
              MinMaxValidator minMax = (MinMaxValidator) validator;
              Long min = minMax.getMin();
              if (min != null) {
                  switch (valueType.getType()) {
                      case STRING:
                      case LIST:
                      case OBJECT:
                          node.get(ModelDescriptionConstants.MIN_LENGTH).set(min);
                          break;
                      default:
                          node.get(ModelDescriptionConstants.MIN).set(min);
                  }
              }
              Long max = minMax.getMax();
              if (max != null) {
                  switch (valueType.getType()) {
                      case STRING:
                      case LIST:
                      case OBJECT:
                          node.get(ModelDescriptionConstants.MAX_LENGTH).set(max);
                          break;
                      default:
                          node.get(ModelDescriptionConstants.MAX).set(max);
                  }
              }
          }
          if (validator instanceof AllowedValuesValidator) {
              AllowedValuesValidator avv = (AllowedValuesValidator) validator;
              List<ModelNode> allowed = avv.getAllowedValues();
              if (allowed != null) {
                  for (ModelNode ok : allowed) {
                      node.get(ModelDescriptionConstants.ALLOWED).add(ok);
                  }
              }
          }


          valueType.addValueTypeDescription(node, prefix, bundle);
      }


      public static final class Builder extends AbstractAttributeDefinitionBuilder<Builder,ObjectListAttributeDefinition>{
          private final ObjectTypeAttributeDefinition valueType;

          public Builder(final String name, final ObjectTypeAttributeDefinition valueType) {
              super(name, ModelType.LIST);
              this.valueType = valueType;
          }

          public static Builder of(final String name, final ObjectTypeAttributeDefinition valueType) {
              return new Builder(name, valueType);
          }

          public ObjectListAttributeDefinition build() {
              if (xmlName == null) xmlName = name;
              if (maxSize < 1) maxSize = Integer.MAX_VALUE;
              return new ObjectListAttributeDefinition(name, xmlName, valueType, allowNull, minSize, maxSize, alternatives, requires, attributeMarshaller,flags);
          }

              /*
        --------------------------
        added for binary compatibility for running compatibility tests
         */
          @Override
          public Builder setAllowNull(boolean allowNull) {
              return super.setAllowNull(allowNull);
          }
      }
  }
