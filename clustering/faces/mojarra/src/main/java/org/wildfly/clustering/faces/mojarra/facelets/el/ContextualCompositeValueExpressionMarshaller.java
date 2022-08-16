/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.faces.mojarra.facelets.el;

import java.io.IOException;
import java.lang.reflect.Field;
import java.security.PrivilegedAction;

import jakarta.el.ValueExpression;
import jakarta.faces.view.Location;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.security.manager.WildFlySecurityManager;

import com.sun.faces.facelets.el.ContextualCompositeValueExpression;

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
