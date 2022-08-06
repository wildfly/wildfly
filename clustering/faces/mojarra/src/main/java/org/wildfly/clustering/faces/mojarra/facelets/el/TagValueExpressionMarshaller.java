/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

import jakarta.el.ValueExpression;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.faces.view.facelets.MockTagAttribute;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.clustering.marshalling.protostream.SimpleObjectOutput;

import com.sun.faces.facelets.el.TagValueExpression;

/**
 * @author Paul Ferraro
 */
public class TagValueExpressionMarshaller implements ProtoStreamMarshaller<TagValueExpression> {

    private static final int ATTRIBUTE_INDEX = 1;
    private static final int EXPRESSION_INDEX = 2;

    @Override
    public Class<? extends TagValueExpression> getJavaClass() {
        return TagValueExpression.class;
    }

    @Override
    public TagValueExpression readFrom(ProtoStreamReader reader) throws IOException {
        String attribute = null;
        ValueExpression expression = null;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case ATTRIBUTE_INDEX:
                    attribute = reader.readString();
                    break;
                case EXPRESSION_INDEX:
                    expression = reader.readAny(ValueExpression.class);
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return new TagValueExpression(new MockTagAttribute(attribute), expression);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, TagValueExpression value) throws IOException {
        String[] strings = new String[1];
        Object[] objects = new Object[1];
        value.writeExternal(new SimpleObjectOutput.Builder().with(strings).with(objects).build());

        String attribute = strings[0];
        if (attribute != null) {
            writer.writeString(ATTRIBUTE_INDEX, attribute);
        }

        Object expression = objects[0];
        if (expression != null) {
            writer.writeAny(EXPRESSION_INDEX, expression);
        }
    }
}
