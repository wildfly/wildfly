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
package org.jboss.as.jmx.model;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXPRESSIONS_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;
import static org.jboss.as.jmx.JmxMessages.MESSAGES;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 * Converts between Open MBean types/data and ModelController types/data
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class TypeConverter {

    private static final SimpleTypeConverter BIG_DECIMAL_NO_EXPR = new SimpleTypeConverter(BigDecimalValueAccessor.INSTANCE, false);
    private static final SimpleTypeConverter BIG_DECIMAL_EXPR = new SimpleTypeConverter(BigDecimalValueAccessor.INSTANCE, true);
    private static final SimpleTypeConverter BIG_INTEGER_NO_EXPR = new SimpleTypeConverter(BigIntegerValueAccessor.INSTANCE, false);
    private static final SimpleTypeConverter BIG_INTEGER_EXPR = new SimpleTypeConverter(BigIntegerValueAccessor.INSTANCE, true);
    private static final SimpleTypeConverter BOOLEAN_NO_EXPR = new SimpleTypeConverter(BooleanValueAccessor.INSTANCE, false);
    private static final SimpleTypeConverter BOOLEAN_EXPR = new SimpleTypeConverter(BooleanValueAccessor.INSTANCE, true);
    private static final SimpleTypeConverter BYTES_NO_EXPR = new SimpleTypeConverter(BytesValueAccessor.INSTANCE, false);
    private static final SimpleTypeConverter DOUBLE_NO_EXPR = new SimpleTypeConverter(DoubleValueAccessor.INSTANCE, false);
    private static final SimpleTypeConverter DOUBLE_EXPR = new SimpleTypeConverter(DoubleValueAccessor.INSTANCE, true);
    private static final SimpleTypeConverter STRING_NO_EXPR = new SimpleTypeConverter(StringValueAccessor.INSTANCE, false);
    private static final SimpleTypeConverter STRING_EXPR = new SimpleTypeConverter(StringValueAccessor.INSTANCE, true);
    //TODO decide how properties should look
    private static final SimpleTypeConverter PROPERTY_NO_EXPR = new SimpleTypeConverter(StringValueAccessor.INSTANCE, false);
    private static final SimpleTypeConverter PROPERTY_EXPR = new SimpleTypeConverter(StringValueAccessor.INSTANCE, true);
    private static final SimpleTypeConverter INT_NO_EXPR = new SimpleTypeConverter(IntegerValueAccessor.INSTANCE, false);
    private static final SimpleTypeConverter INT_EXPR = new SimpleTypeConverter(IntegerValueAccessor.INSTANCE, true);
    private static final SimpleTypeConverter LONG_NO_EXPR = new SimpleTypeConverter(LongValueAccessor.INSTANCE, false);
    private static final SimpleTypeConverter LONG_EXPR = new SimpleTypeConverter(LongValueAccessor.INSTANCE, true);
    private static final SimpleTypeConverter TYPE_NO_EXPR = new SimpleTypeConverter(ModelTypeValueAccessor.INSTANCE, false);
    private static final SimpleTypeConverter TYPE_EXPR = new SimpleTypeConverter(ModelTypeValueAccessor.INSTANCE, true);
    private static final SimpleTypeConverter UNDEFINED_NO_EXPR = new SimpleTypeConverter(UndefinedValueAccessor.INSTANCE, false);
    private static final SimpleTypeConverter UNDEFINED_EXPR = new SimpleTypeConverter(UndefinedValueAccessor.INSTANCE, true);

    public abstract OpenType<?> getOpenType();
    public abstract Object fromModelNode(final ModelNode node);
    public abstract ModelNode toModelNode(final Object o);
    public abstract Object[] toArray(final List<Object> list);

    static OpenType<?> convertToMBeanType(final ModelNode description) {
        return getConverter(description).getOpenType();
    }

    static ModelNode toModelNode(final ModelNode description, final Object value) {
        ModelNode node = new ModelNode();
        if (value == null) {
            return node;
        }
        return getConverter(description).toModelNode(value);
    }

    static Object fromModelNode(final ModelNode description, final ModelNode value) {
        if (value == null || !value.isDefined()) {
            return null;
        }
        return getConverter(description).fromModelNode(value);
    }

    public static ModelType getType(ModelNode typeNode) {
        if (typeNode == null) {
            return ModelType.UNDEFINED;
        }
        try {
            return ModelType.valueOf(typeNode.toString());
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static TypeConverter getConverter(ModelNode description) {
        return getConverter(description.hasDefined(TYPE) ? description.get(TYPE) : null,
                description.hasDefined(VALUE_TYPE) ? description.get(VALUE_TYPE) : null,
                areExpressionsAllowed(description));
    }

    static TypeConverter getConverter(ModelNode typeNode, ModelNode valueTypeNode, boolean expressionsAllowed) {
        ModelType modelType = getType(typeNode);
        if (modelType == null) {
            return new ComplexTypeConverter(typeNode);
        }

        switch (modelType) {
        case BIG_DECIMAL:
            return expressionsAllowed ? BIG_DECIMAL_EXPR: BIG_DECIMAL_NO_EXPR;
        case BIG_INTEGER:
            return expressionsAllowed ? BIG_INTEGER_EXPR : BIG_INTEGER_NO_EXPR;
        case BOOLEAN:
            return expressionsAllowed ? BOOLEAN_EXPR : BOOLEAN_NO_EXPR;
        case BYTES:
            //Allowing expressions for byte[] seems pointless
            return BYTES_NO_EXPR;
        case DOUBLE:
            return expressionsAllowed ? DOUBLE_EXPR : DOUBLE_NO_EXPR;
        case STRING:
            return expressionsAllowed ? STRING_EXPR : STRING_NO_EXPR;
        case PROPERTY:
            return expressionsAllowed ? PROPERTY_EXPR : PROPERTY_NO_EXPR;
        case INT:
            return expressionsAllowed ? INT_EXPR : INT_NO_EXPR;
        case LONG:
            return expressionsAllowed ? LONG_EXPR : LONG_NO_EXPR;
        case TYPE:
            return expressionsAllowed ? TYPE_EXPR : TYPE_NO_EXPR;
        case UNDEFINED:
            return expressionsAllowed ? UNDEFINED_EXPR : UNDEFINED_NO_EXPR;
        case OBJECT:
            return new ObjectTypeConverter(valueTypeNode, expressionsAllowed);
        case LIST:
            return new ListTypeConverter(valueTypeNode, expressionsAllowed);
        default:
            throw MESSAGES.unknownType(modelType);
        }
    }


    private static boolean areExpressionsAllowed(ModelNode description) {
        if (description.hasDefined(EXPRESSIONS_ALLOWED)) {
            return description.get(EXPRESSIONS_ALLOWED).asBoolean();
        }
        return false;
    }



    private static ModelNode nullNodeAsUndefined(ModelNode node) {
        if (node == null) {
            return new ModelNode();
        }
        return node;
    }

    private static class SimpleTypeConverter extends TypeConverter {
        private final SimpleValueAccessor valueAccessor;
        private final boolean expressions;

        public SimpleTypeConverter(SimpleValueAccessor valueAccessor, boolean expressions) {
            this.valueAccessor = valueAccessor;
            this.expressions = expressions;
        }

        @Override
        public OpenType<?> getOpenType() {
            if (!expressions) {
                return valueAccessor.getOpenType();
            } else {
                return SimpleType.STRING;
            }

        }

        @Override
        public Object fromModelNode(final ModelNode node) {
            if (!expressions || valueAccessor == UndefinedValueAccessor.INSTANCE) {
                if (node == null || !node.isDefined() || node.asString().isEmpty()) {
                    return null;
                }
                return valueAccessor.fromModelNode(node);
            } else {
                return node.asString();
            }
        }

        @Override
        public ModelNode toModelNode(final Object o) {
            if (o == null) {
                return new ModelNode();
            }
            if (expressions) {
                String s = (String)o;
                int start = s.indexOf("${");
                if (start != -1 && s.indexOf('}', start) != -1) {
                    return new ModelNode().setExpression(s);
                }
                return valueAccessor.toModelNode(valueAccessor.parseFromNonExpressionString(s));
            } else {
                return valueAccessor.toModelNode(o);
            }
        }

        public Object[] toArray(final List<Object> list) {
            if (expressions) {
                return list.toArray(new String[list.size()]);
            } else {
                return valueAccessor.toArray(list);
            }
        }
    }

    private static class ObjectTypeConverter extends TypeConverter {

        final ModelNode valueTypeNode;
        final ModelType valueType;
        final boolean expressionsAllowed;
        OpenType<?> openType;

        ObjectTypeConverter(ModelNode valueTypeNode, boolean expressionsAllowed) {
            this.valueTypeNode = nullNodeAsUndefined(valueTypeNode);
            ModelType valueType = getType(valueTypeNode);
            this.valueType = valueType == ModelType.UNDEFINED ? null : valueType;
            this.expressionsAllowed = expressionsAllowed;
        }

        @Override
        public OpenType<?>  getOpenType() {
            if (openType != null) {
                return openType;
            }
            openType = getConverter(valueTypeNode, null, expressionsAllowed).getOpenType();
            if (valueType == null && (openType instanceof CompositeType || !valueTypeNode.isDefined())) {
                //For complex value types just return the composite type
                return openType;
            }
            try {
                CompositeType rowType = new CompositeType(
                        MESSAGES.compositeEntryTypeName(),
                        MESSAGES.compositeEntryTypeDescription(),
                        new String[] {"key", "value"},
                        new String[] {MESSAGES.compositeEntryKeyDescription(), MESSAGES.compositeEntryValueDescription()},
                        new OpenType[] {SimpleType.STRING, openType});
                openType = new TabularType(MESSAGES.compositeMapName(), MESSAGES.compositeMapDescription(), rowType, new String[] {"key"});
                return openType;
            } catch (OpenDataException e1) {
                throw new RuntimeException(e1);
            }
        }

        @Override
        public Object fromModelNode(final ModelNode node) {
            if (node == null || !node.isDefined()) {
                return null;
            }
            if (valueType != null) {
                return fromSimpleModelNode(node);
            } else {
                TypeConverter converter = getConverter(valueTypeNode, null, expressionsAllowed);
                return converter.fromModelNode(node);
            }
        }

        Object fromSimpleModelNode(final ModelNode node) {
            final TabularType tabularType = (TabularType)getOpenType();
            final TabularDataSupport tabularData = new TabularDataSupport(tabularType);
            final Map<String, ModelNode> values = new HashMap<String, ModelNode>();
            final List<Property> properties = node.isDefined() ? node.asPropertyList() : null;
            if (properties != null) {
                for (Property prop : properties) {
                    values.put(prop.getName(), prop.getValue());
                }
            }

            final TypeConverter converter = TypeConverter.getConverter(valueTypeNode, null, expressionsAllowed);
            for (Map.Entry<String, ModelNode> prop : values.entrySet()) {
                Map<String, Object> rowData = new HashMap<String, Object>();
                rowData.put("key", prop.getKey());
                rowData.put("value", converter.fromModelNode(prop.getValue()));
                try {
                    tabularData.put(new CompositeDataSupport(tabularType.getRowType(), rowData));
                } catch (OpenDataException e) {
                    throw new RuntimeException(e);
                }
            }
            return tabularData;
        }

        @Override
        public ModelNode toModelNode(Object o) {
            if (o == null) {
                return new ModelNode();
            }
            if (valueType == null) {
                //complex
                return TypeConverter.getConverter(valueTypeNode, null, expressionsAllowed).toModelNode(o);
            } else {
                //map
                final ModelNode node = new ModelNode();
                final TypeConverter converter = TypeConverter.getConverter(valueTypeNode, null, expressionsAllowed);
                for (Map.Entry<String, Object> entry : ((Map<String, Object>)o).entrySet()) {
                    entry = convertTabularTypeEntryToMapEntry(entry);
                    node.get(entry.getKey()).set(converter.toModelNode(entry.getValue()));
                }
                return node;
            }
        }

        @Override
        public Object[] toArray(List<Object> list) {
            if (getOpenType() == SimpleType.STRING) {
                return list.toArray(new String[list.size()]);
            }
            return list.toArray(new Object[list.size()]);
        }

        //TODO this may go depending on if we want to force tabular types only
        private Map.Entry<String, Object> convertTabularTypeEntryToMapEntry(final Map.Entry<?, Object> entry) {
            if (entry.getKey() instanceof List) {
                //It comes from a TabularType
                return new Map.Entry<String, Object>() {

                    @Override
                    public String getKey() {
                        List<?> keyList = (List<?>)entry.getKey();
                        if (keyList.size() != 1) {
                            throw MESSAGES.invalidKey(keyList, entry);
                        }
                        return (String)keyList.get(0);
                    }

                    @Override
                    public Object getValue() {
                        return ((CompositeDataSupport)entry.getValue()).get("value");
                    }

                    @Override
                    public Object setValue(Object value) {
                        throw new UnsupportedOperationException();
                    }

                };
            } else {
                return new Map.Entry<String, Object>() {

                    @Override
                    public String getKey() {
                        return (String)entry.getKey();
                    }

                    @Override
                    public Object getValue() {
                        return entry.getValue();
                    }

                    @Override
                    public Object setValue(Object value) {
                        throw new UnsupportedOperationException();
                    }
                };
            }

        }
    }

    private static class ListTypeConverter extends TypeConverter {
        final ModelNode valueTypeNode;
        final boolean expressionsAllowed;

        ListTypeConverter(ModelNode valueTypeNode, boolean expressionsAllowed) {
            this.valueTypeNode = nullNodeAsUndefined(valueTypeNode);
            this.expressionsAllowed = expressionsAllowed;
        }

        @Override
        public OpenType<?> getOpenType() {
            try {
                return ArrayType.getArrayType(getConverter(valueTypeNode, null, expressionsAllowed).getOpenType());
            } catch (OpenDataException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Object fromModelNode(final ModelNode node) {
            if (node == null || !node.isDefined()) {
                return null;
            }
            final List<Object> list = new ArrayList<Object>();
            final TypeConverter converter = getConverter(valueTypeNode, null, expressionsAllowed);
            for (ModelNode element : node.asList()) {
                list.add(converter.fromModelNode(element));
            }
            return converter.toArray(list);
        }

        @Override
        public ModelNode toModelNode(Object o) {
            if (o == null) {
                return new ModelNode();
            }
            ModelNode node = new ModelNode();
            final TypeConverter converter = getConverter(valueTypeNode, null, expressionsAllowed);
            for (Object value : (Object[])o) {
                node.add(converter.toModelNode(value));
            }
            return node;
        }

        @Override
        public Object[] toArray(List<Object> list) {
            return null;
        }
    }

    private static class ComplexTypeConverter extends TypeConverter {
        final ModelNode typeNode;

        ComplexTypeConverter(final ModelNode typeNode) {
            this.typeNode = nullNodeAsUndefined(typeNode);
        }

        @Override
        public OpenType<?> getOpenType() {
            List<String> itemNames = new ArrayList<String>();
            List<String> itemDescriptions = new ArrayList<String>();
            List<OpenType<?>> itemTypes = new ArrayList<OpenType<?>>();

            //Some of the common operation descriptions use value-types like "The type will be that of the attribute found".
            if (!typeNode.isDefined() || typeNode.getType() == ModelType.STRING) {
                return SimpleType.STRING;
            }

            for (String name : typeNode.keys()) {
                ModelNode current = typeNode.get(name);
                itemNames.add(name);
                String description = null;
                if (!current.hasDefined(DESCRIPTION)) {
                    description = "-";
                }
                else {
                    description = current.get(DESCRIPTION).asString().trim();
                    if (description.length() == 0) {
                        description = "-";
                    }
                }

                itemDescriptions.add(getDescription(current));
                itemTypes.add(getConverter(current).getOpenType());
            }
            try {
                return new CompositeType(MESSAGES.complexCompositeEntryTypeName(),
                        MESSAGES.complexCompositeEntryTypeDescription(),
                        itemNames.toArray(new String[itemNames.size()]),
                        itemDescriptions.toArray(new String[itemDescriptions.size()]),
                        itemTypes.toArray(new OpenType[itemTypes.size()]));
            } catch (OpenDataException e) {
                throw new RuntimeException(e);
            }
        }

        static String getDescription(ModelNode node) {
            if (!node.hasDefined(DESCRIPTION)) {
                return "-";
            }
            String description = node.get(DESCRIPTION).asString();
            if (description.trim().length() == 0) {
                return "-";
            }
            return description;
        }
        @Override
        public Object fromModelNode(ModelNode node) {
            if (node == null || !node.isDefined()) {
                return null;
            }

            final OpenType<?> openType = getOpenType();
            if (openType instanceof CompositeType) {
                final CompositeType compositeType = (CompositeType)openType;
                //Create a composite
                final Map<String, Object> items = new HashMap<String, Object>();
                for (String attrName : compositeType.keySet()) {
                    TypeConverter converter = getConverter(typeNode.get(attrName, TYPE), typeNode.get(attrName, VALUE_TYPE), areExpressionsAllowed(typeNode.get(attrName)));
                    items.put(attrName, converter.fromModelNode(node.get(attrName)));
                }

                try {
                    return new CompositeDataSupport(compositeType, items);
                } catch (OpenDataException e) {
                    throw new RuntimeException(e);
                }
            } else {
                return node.toJSONString(false);
            }
        }

        @Override
        public ModelNode toModelNode(Object o) {
            if (o == null) {
                return new ModelNode();
            }
            if (o instanceof CompositeData) {
                final ModelNode node = new ModelNode();
                final CompositeData composite = (CompositeData)o;
                for (String key : composite.getCompositeType().keySet()) {
                    if (!typeNode.hasDefined(key)){
                        throw MESSAGES.unknownValue(key);
                    }
                    TypeConverter converter = getConverter(typeNode.get(key, TYPE), typeNode.get(key, VALUE_TYPE), areExpressionsAllowed(typeNode.get(key)));
                    node.get(key).set(converter.toModelNode(composite.get(key)));
                }
                return node;
            } else {
                return ModelNode.fromJSONString((String)o);
            }
        }

        @Override
        public Object[] toArray(List<Object> list) {
            return list.toArray(new CompositeData[list.size()]);
        }
    }

    private abstract static class SimpleValueAccessor {
        abstract OpenType<?> getOpenType();
        abstract Object fromModelNode(ModelNode node);
        abstract ModelNode toModelNode(Object o);
        abstract Object[] toArray(final List<Object> list);
        abstract Object parseFromNonExpressionString(String s);
    }

    private static class BigDecimalValueAccessor extends SimpleValueAccessor {
        static final BigDecimalValueAccessor INSTANCE = new BigDecimalValueAccessor();

        OpenType<?> getOpenType() {
            return SimpleType.BIGDECIMAL;
        }

        Object fromModelNode(final ModelNode node) {
            return node.asBigDecimal();
        }

        ModelNode toModelNode(final Object o) {
            return new ModelNode().set((BigDecimal)o);
        }

        Object[] toArray(final List<Object> list) {
            return list.toArray(new BigDecimal[list.size()]);
        }

        Object parseFromNonExpressionString(String s) {
            return new BigDecimal(s);
        }
    }

    private static class BigIntegerValueAccessor extends SimpleValueAccessor {
        static final BigIntegerValueAccessor INSTANCE = new BigIntegerValueAccessor();

        OpenType<?> getOpenType() {
            return SimpleType.BIGINTEGER;
        }

        Object fromModelNode(final ModelNode node) {
            return node.asBigInteger();
        }

        ModelNode toModelNode(final Object o) {
            return new ModelNode().set((BigInteger)o);
        }

        Object[] toArray(final List<Object> list) {
            return list.toArray(new BigInteger[list.size()]);
        }

        Object parseFromNonExpressionString(String s) {
            return new BigInteger(s);
        }
    }

    private static class BooleanValueAccessor extends SimpleValueAccessor {
        static final BooleanValueAccessor INSTANCE = new BooleanValueAccessor();

        OpenType<?> getOpenType() {
            return SimpleType.BOOLEAN;
        }

        Object fromModelNode(final ModelNode node) {
            return node.asBoolean();
        }

        ModelNode toModelNode(final Object o) {
            return new ModelNode().set((Boolean)o);
        }

        Object[] toArray(final List<Object> list) {
            return list.toArray(new Boolean[list.size()]);
        }

        Object parseFromNonExpressionString(String s) {
            return Boolean.parseBoolean(s);
        }
    }

    private static class BytesValueAccessor extends SimpleValueAccessor {
        static final BytesValueAccessor INSTANCE = new BytesValueAccessor();
        static final ArrayType<byte[]> ARRAY_TYPE = ArrayType.getPrimitiveArrayType(byte[].class);

        OpenType<?> getOpenType() {
            return ARRAY_TYPE;
        }

        Object fromModelNode(final ModelNode node) {
            return node.resolve().asBytes();
        }

        ModelNode toModelNode(final Object o) {
            return new ModelNode().set((byte[])o);
        }

        Object[] toArray(final List<Object> list) {
            return list.toArray(new byte[list.size()][]);
        }

        Object parseFromNonExpressionString(String s) {
            return s.getBytes();
        }

    }

    private static class DoubleValueAccessor extends SimpleValueAccessor {
        static final DoubleValueAccessor INSTANCE = new DoubleValueAccessor();

        OpenType<?> getOpenType() {
            return SimpleType.DOUBLE;
        }

        Object fromModelNode(final ModelNode node) {
            return node.asDouble();
        }

        ModelNode toModelNode(final Object o) {
            return new ModelNode().set((Double)o);
        }

        Object[] toArray(final List<Object> list) {
            return list.toArray(new Double[list.size()]);
        }

        Object parseFromNonExpressionString(String s) {
            return Double.parseDouble(s);
        }
    }


    private static class IntegerValueAccessor extends SimpleValueAccessor {
        static final IntegerValueAccessor INSTANCE = new IntegerValueAccessor();

        OpenType<?> getOpenType() {
            return SimpleType.INTEGER;
        }

        Object fromModelNode(final ModelNode node) {
            return node.asInt();
        }

        ModelNode toModelNode(final Object o) {
            return new ModelNode().set((Integer)o);
        }

        Object[] toArray(final List<Object> list) {
            return list.toArray(new Integer[list.size()]);
        }

        Object parseFromNonExpressionString(String s) {
            return Integer.parseInt(s);
        }
    }

    private static class StringValueAccessor extends SimpleValueAccessor {
        static final StringValueAccessor INSTANCE = new StringValueAccessor();

        OpenType<?> getOpenType() {
            return SimpleType.STRING;
        }

        Object fromModelNode(final ModelNode node) {
            return node.asString();
        }

        ModelNode toModelNode(final Object o) {
            if (o == null) {
                return new ModelNode();
            }
            return new ModelNode().set((String)o);
        }

        Object[] toArray(final List<Object> list) {
            return list.toArray(new String[list.size()]);
        }

        Object parseFromNonExpressionString(String s) {
            return s;
        }
    }

    private static class UndefinedValueAccessor extends SimpleValueAccessor {
        static final UndefinedValueAccessor INSTANCE = new UndefinedValueAccessor();

        OpenType<?> getOpenType() {
            return SimpleType.STRING;
        }

        Object fromModelNode(final ModelNode node) {
            return node.toJSONString(false);
        }

        ModelNode toModelNode(final Object o) {
            return ModelNode.fromJSONString((String)o);
        }

        Object[] toArray(final List<Object> list) {
            return list.toArray(new String[list.size()]);
        }

        Object parseFromNonExpressionString(String s) {
            return s;
        }
    }

//    private static class ExpressionTypeConverter extends StringTypeConverter {
//        static final ExpressionTypeConverter INSTANCE = new ExpressionTypeConverter();
//        //TODO this is probably fine?
//    }
//
//    private static class PropertyTypeConverter extends StringTypeConverter {
//        static final ExpressionTypeConverter INSTANCE = new ExpressionTypeConverter();
//        //TODO Decide how these should look
//    }

    private static class LongValueAccessor extends SimpleValueAccessor {
        static final LongValueAccessor INSTANCE = new LongValueAccessor();

        OpenType<?> getOpenType() {
            return SimpleType.LONG;
        }

        Object fromModelNode(final ModelNode node) {
            return node.asLong();
        }

        ModelNode toModelNode(final Object o) {
            return new ModelNode().set((Long)o);
        }

        Object[] toArray(final List<Object> list) {
            return list.toArray(new Long[list.size()]);
        }

        @Override
        Object parseFromNonExpressionString(String s) {
            return Long.parseLong(s);
        }
    }

    private static class ModelTypeValueAccessor extends SimpleValueAccessor {
        static final ModelTypeValueAccessor INSTANCE = new ModelTypeValueAccessor();

        OpenType<?> getOpenType() {
            return SimpleType.STRING;
        }

        Object fromModelNode(final ModelNode node) {
            return node.asString();
        }

        ModelNode toModelNode(final Object o) {
            return new ModelNode().set(ModelType.valueOf((String)o));
        }

        Object[] toArray(final List<Object> list) {
            return list.toArray(new String[list.size()]);
        }

        Object parseFromNonExpressionString(String s) {
            return ModelType.valueOf(s);
        }
    }
}