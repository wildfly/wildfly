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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;
import static org.jboss.as.jmx.JmxMessages.MESSAGES;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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

    private static final Pattern VAULT_PATTERN = Pattern.compile("\\$\\{VAULT::.*::.*::.*\\}");

    public abstract OpenType<?> getOpenType();
    public abstract Object fromModelNode(final ModelNode node);
    public abstract ModelNode toModelNode(final Object o);
    public abstract Object[] toArray(final List<Object> list);

    static OpenType<?> convertToMBeanType(final ModelNode description) {
        return getConverter(
                description.hasDefined(TYPE) ? description.get(TYPE) : null,
                description.hasDefined(VALUE_TYPE) ? description.get(VALUE_TYPE) : null).getOpenType();
    }

    static ModelNode toModelNode(final ModelNode description, final Object value) {
        ModelNode node = new ModelNode();
        if (value == null) {
            return node;
        }
        final ModelNode typeNode = description.hasDefined(TYPE) ? description.get(TYPE) : null;
        return getConverter(typeNode, description.hasDefined(VALUE_TYPE) ? description.get(VALUE_TYPE) : null).toModelNode(value);
    }

    static Object fromModelNode(final ModelNode description, final ModelNode value) {
        if (value == null || !value.isDefined()) {
            return null;
        }
        final ModelNode typeNode = description.hasDefined(TYPE) ? description.get(TYPE) : null;
        final ModelNode valueNode = description.hasDefined(VALUE_TYPE) ? description.get(VALUE_TYPE) : null;
        return getConverter(typeNode, valueNode).fromModelNode(value);
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

    public static TypeConverter getConverter(ModelNode typeNode, ModelNode valueTypeNode) {
        ModelType modelType = getType(typeNode);
        if (modelType == null) {
            return new ComplexTypeConverter(typeNode);
        }
        switch (modelType) {
        case BIG_DECIMAL:
            return BigDecimalTypeConverter.INSTANCE;
        case BIG_INTEGER:
            return BigIntegerTypeConverter.INSTANCE;
        case BOOLEAN:
            return BooleanTypeConverter.INSTANCE;
        case BYTES:
            return BytesTypeConverter.INSTANCE;
        case DOUBLE:
            return DoubleTypeConverter.INSTANCE;
        case EXPRESSION:
            return ExpressionTypeConverter.INSTANCE;
        case STRING:
            return StringTypeConverter.INSTANCE;
        case PROPERTY:
            return PropertyTypeConverter.INSTANCE;
        case INT:
            return IntegerTypeConverter.INSTANCE;
        case LONG:
            return LongTypeConverter.INSTANCE;
        case TYPE:
            return ModelTypeTypeConverter.INSTANCE;
        case OBJECT:
            return new ObjectTypeConverter(valueTypeNode);
        case LIST:
            return new ListTypeConverter(valueTypeNode);
        case UNDEFINED:
            return UndefinedTypeConverter.INSTANCE;
        default:
            throw MESSAGES.unknownType(modelType);
        }
    }

    private static ModelNode nullNodeAsUndefined(ModelNode node) {
        if (node == null) {
            return new ModelNode();
        }
        return node;
    }


    protected ModelNode resolveSimpleType(ModelNode node) {
        if (node.getType() == ModelType.EXPRESSION) {
            return node.resolve();
        }
        return node;
    }


    private abstract static class SimpleTypeConverter extends TypeConverter {
        @Override
        public Object fromModelNode(final ModelNode node) {
            if (node == null || !node.isDefined() || node.asString().length() == 0) {
                return null;
            }

            if (node.getType() == ModelType.EXPRESSION) {
                if (!VAULT_PATTERN.matcher(node.asString()).matches()) {
                    return internalFromModelNode(node.resolve());
                }
            }
            return internalFromModelNode(node);
        }

        public ModelNode toModelNode(final Object o) {
            if (o == null) {
                return new ModelNode();
            }
            boolean possibleExpression = false;
            if (o instanceof String) {
                possibleExpression = isPossibleExpression((String)o);
            }
            if (possibleExpression) {
                return createPossibleExpression((String)o);
            }

            try {
                return internalToModelNode(o);
            } catch (ClassCastException e) {
                throw new IllegalArgumentException(e);
            }
        }

        abstract ModelNode internalToModelNode(final Object o);

        abstract Object internalFromModelNode(final ModelNode node);

        private boolean isPossibleExpression(String s) {
            int start = s.indexOf("${");
            return start != -1 && s.indexOf('}', start) != -1;
        }

        ModelNode createPossibleExpression(String s) {
            throw MESSAGES.expressionCannotBeConvertedIntoTargeteType(getOpenType());
        }
    }


    private static class BigDecimalTypeConverter extends SimpleTypeConverter {
        static final BigDecimalTypeConverter INSTANCE = new BigDecimalTypeConverter();

        @Override
        public OpenType<?> getOpenType() {
            return SimpleType.BIGDECIMAL;
        }

        @Override
        Object internalFromModelNode(final ModelNode node) {
            return node.asBigDecimal();
        }

        @Override
        public ModelNode internalToModelNode(final Object o) {
            return new ModelNode().set((BigDecimal)o);
        }

        public Object[] toArray(final List<Object> list) {
            return list.toArray(new BigDecimal[list.size()]);
        }
    }

    private static class BigIntegerTypeConverter extends SimpleTypeConverter {
        static final BigIntegerTypeConverter INSTANCE = new BigIntegerTypeConverter();

        @Override
        public OpenType<?> getOpenType() {
            return SimpleType.BIGINTEGER;
        }

        @Override
        Object internalFromModelNode(final ModelNode node) {
            return node.asBigInteger();
        }

        public ModelNode internalToModelNode(final Object o) {
            return new ModelNode().set((BigInteger)o);
        }

        public Object[] toArray(final List<Object> list) {
            return list.toArray(new BigInteger[list.size()]);
        }
    }

    private static class BooleanTypeConverter extends SimpleTypeConverter {
        static final BooleanTypeConverter INSTANCE = new BooleanTypeConverter();

        @Override
        public OpenType<?> getOpenType() {
            return SimpleType.BOOLEAN;
        }

        @Override
        Object internalFromModelNode(final ModelNode node) {
            return Boolean.valueOf(node.asBoolean());
        }

        @Override
        public ModelNode internalToModelNode(final Object o) {
            return new ModelNode().set((Boolean)o);
        }

        public Object[] toArray(final List<Object> list) {
            return list.toArray(new Boolean[list.size()]);
        }
    }

    private static class BytesTypeConverter extends TypeConverter {
        static final BytesTypeConverter INSTANCE = new BytesTypeConverter();
        static final ArrayType<byte[]> ARRAY_TYPE = ArrayType.getPrimitiveArrayType(byte[].class);

        @Override
        public OpenType<?> getOpenType() {
            return ARRAY_TYPE;
        }

        @Override
        public Object fromModelNode(final ModelNode node) {
            if (node == null || !node.isDefined()) {
                return null;
            }
            return node.asBytes();
        }

        @Override
        public ModelNode toModelNode(final Object o) {
            if (o == null) {
                return new ModelNode();
            }
            try {
                return new ModelNode().set((byte[])o);
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }

        public Object[] toArray(final List<Object> list) {
            return list.toArray(new byte[list.size()][]);
        }
    }

    private static class DoubleTypeConverter extends SimpleTypeConverter {
        static final DoubleTypeConverter INSTANCE = new DoubleTypeConverter();

        @Override
        public OpenType<?> getOpenType() {
            return SimpleType.DOUBLE;
        }

        @Override
        Object internalFromModelNode(final ModelNode node) {
            return Double.valueOf(node.asDouble());
        }

        @Override
        public ModelNode internalToModelNode(final Object o) {
            return new ModelNode().set((Double)o);
        }

        public Object[] toArray(final List<Object> list) {
            return list.toArray(new Double[list.size()]);
        }
    }


    private static class IntegerTypeConverter extends SimpleTypeConverter {
        static final IntegerTypeConverter INSTANCE = new IntegerTypeConverter();

        @Override
        public OpenType<?> getOpenType() {
            return SimpleType.INTEGER;
        }

        @Override
        Object internalFromModelNode(final ModelNode node) {
            return Integer.valueOf(node.asInt());
        }

        @Override
        public ModelNode internalToModelNode(final Object o) {
            return new ModelNode().set((Integer)o);
        }

        public Object[] toArray(final List<Object> list) {
            return list.toArray(new Integer[list.size()]);
        }
    }

    private static class StringTypeConverter extends SimpleTypeConverter {
        static final StringTypeConverter INSTANCE = new StringTypeConverter();

        @Override
        public OpenType<?> getOpenType() {
            return SimpleType.STRING;
        }

        @Override
        Object internalFromModelNode(final ModelNode node) {
            return node.asString();
        }

        @Override
        public ModelNode internalToModelNode(final Object o) {
            return new ModelNode().set((String)o);
        }

        public Object[] toArray(final List<Object> list) {
            return list.toArray(new String[list.size()]);
        }

        @Override
        ModelNode createPossibleExpression(String s) {
            return new ModelNode().setExpression(s);
        }
    }

    private static class UndefinedTypeConverter extends SimpleTypeConverter {
        static final UndefinedTypeConverter INSTANCE = new UndefinedTypeConverter();

        @Override
        public OpenType<?> getOpenType() {
            return SimpleType.STRING;
        }

        @Override
        Object internalFromModelNode(final ModelNode node) {
            return node.toJSONString(false);
        }

        @Override
        public ModelNode internalToModelNode(final Object o) {
            return ModelNode.fromJSONString((String)o);
        }

        public Object[] toArray(final List<Object> list) {
            return list.toArray(new String[list.size()]);
        }
    }

    private static class ExpressionTypeConverter extends StringTypeConverter {
        static final ExpressionTypeConverter INSTANCE = new ExpressionTypeConverter();
        //TODO this is probably fine?
    }

    private static class PropertyTypeConverter extends StringTypeConverter {
        static final ExpressionTypeConverter INSTANCE = new ExpressionTypeConverter();
        //TODO Decide how these should look
    }

    private static class LongTypeConverter extends SimpleTypeConverter {
        static final LongTypeConverter INSTANCE = new LongTypeConverter();

        @Override
        public OpenType<?> getOpenType() {
            return SimpleType.LONG;
        }

        @Override
        Object internalFromModelNode(final ModelNode node) {
            return Long.valueOf(node.asLong());
        }

        @Override
        public ModelNode internalToModelNode(final Object o) {
            return new ModelNode().set((Long)o);
        }

        public Object[] toArray(final List<Object> list) {
            return list.toArray(new Long[list.size()]);
        }
    }

    private static class ModelTypeTypeConverter extends SimpleTypeConverter {
        static final ModelTypeTypeConverter INSTANCE = new ModelTypeTypeConverter();

        @Override
        public OpenType<?> getOpenType() {
            return SimpleType.STRING;
        }

        @Override
        Object internalFromModelNode(final ModelNode node) {
            return String.valueOf(node.asString());
        }

        @Override
        public ModelNode internalToModelNode(final Object o) {
            return new ModelNode().set(ModelType.valueOf((String)o));
        }

        public Object[] toArray(final List<Object> list) {
            return list.toArray(new String[list.size()]);
        }
    }

    private static class ObjectTypeConverter extends TypeConverter {

        final ModelNode valueTypeNode;
        final ModelType valueType;
        OpenType<?> openType;

        ObjectTypeConverter(ModelNode valueTypeNode) {
            this.valueTypeNode = nullNodeAsUndefined(valueTypeNode);
            ModelType valueType = getType(valueTypeNode);
            this.valueType = valueType == ModelType.UNDEFINED ? null : valueType;
        }

        @Override
        public OpenType<?>  getOpenType() {
            if (openType != null) {
                return openType;
            }
            openType = getConverter(valueTypeNode, null).getOpenType();
            if (openType instanceof CompositeType || !valueTypeNode.isDefined()) {
                //For complex value types just return the composite type
                return openType;
            }
            try {
                CompositeType rowType = new CompositeType(
                        "entry",
                        "An entry",
                        new String[] {"key", "value"},
                        new String[] {"The key", "The value"},
                        new OpenType[] {SimpleType.STRING, openType});
                openType = new TabularType("A map", "The map is indexed by 'key'", rowType, new String[] {"key"});
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
                TypeConverter converter = getConverter(valueTypeNode, null);
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

            final TypeConverter converter = TypeConverter.getConverter(valueTypeNode, null);
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
                return TypeConverter.getConverter(valueTypeNode, null).toModelNode(o);
            } else {
                //map
                final ModelNode node = new ModelNode();
                final TypeConverter converter = TypeConverter.getConverter(valueTypeNode, null);
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

        ListTypeConverter(ModelNode valueTypeNode) {
            this.valueTypeNode = nullNodeAsUndefined(valueTypeNode);
        }

        @Override
        public OpenType<?> getOpenType() {
            try {
                return ArrayType.getArrayType(getConverter(valueTypeNode, null).getOpenType());
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
            final TypeConverter converter = getConverter(valueTypeNode, null);
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
            final TypeConverter converter = getConverter(valueTypeNode, null);
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
                itemTypes.add(getConverter(current.get(TYPE), current.get(VALUE_TYPE)).getOpenType());
            }
            try {
                return new CompositeType("Complex type", "A complex type", itemNames.toArray(new String[itemNames.size()]), itemDescriptions.toArray(new String[itemDescriptions.size()]), itemTypes.toArray(new OpenType[itemTypes.size()]));
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
                    TypeConverter converter = getConverter(typeNode.get(attrName, TYPE), typeNode.get(attrName, VALUE_TYPE));
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
                    final ModelNode type = typeNode.get(key).get(TYPE);
                    final ModelNode valueType = typeNode.get(key).get(VALUE_TYPE);
                    TypeConverter converter = getConverter(type, valueType);
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

}