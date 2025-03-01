/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.faces.mojarra.facelets.el;

import java.io.IOException;
import java.lang.reflect.Field;
import java.security.PrivilegedAction;

import jakarta.el.ValueExpression;
import jakarta.faces.view.Location;

import com.sun.faces.facelets.el.ContextualCompositeValueExpression;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author Paul Ferraro
 */
public class ContextualCompositeValueExpressionMarshaller implements ProtoStreamMarshaller<ContextualCompositeValueExpression> {

    private static final int LOCATION_INDEX = 1;
    private static final int EXPRESSION_INDEX = 2;

    static final Field VALUE_EXPRESSION_FIELD = WildFlySecurityManager.doUnchecked(new PrivilegedAction<>() {
            @Override
            public Field run() {
                for (Field field : ContextualCompositeValueExpression.class.getDeclaredFields()) {
                    if (field.getType() == ValueExpression.class) {
                        field.setAccessible(true);
                        return field;
                    }
                }
                throw new IllegalArgumentException(ValueExpression.class.getName());
            }
        });

    @Override
    public Class<? extends ContextualCompositeValueExpression> getJavaClass() {
        return ContextualCompositeValueExpression.class;
    }

    @Override
    public ContextualCompositeValueExpression readFrom(ProtoStreamReader reader) throws IOException {
        ValueExpression expression = null;
        Location location = null;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case LOCATION_INDEX:
                    location = reader.readObject(Location.class);
                    break;
                case EXPRESSION_INDEX:
                    expression = reader.readAny(ValueExpression.class);
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return new ContextualCompositeValueExpression(location, expression);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, ContextualCompositeValueExpression value) throws IOException {
        Location location = value.getLocation();
        if (location != null) {
            writer.writeObject(LOCATION_INDEX, location);
        }
        ValueExpression expression = WildFlySecurityManager.doUnchecked(new PrivilegedAction<>() {
            @Override
            public ValueExpression run() {
                try {
                    return (ValueExpression) VALUE_EXPRESSION_FIELD.get(value);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
        if (expression != null) {
            writer.writeAny(EXPRESSION_INDEX, expression);
        }
    }
}
