/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.faces.mojarra.facelets.el;

import java.io.IOException;

import jakarta.el.MethodExpression;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.SimpleObjectOutput;
import org.wildfly.clustering.faces.view.facelets.MockTagAttribute;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

import com.sun.faces.facelets.el.TagMethodExpression;

/**
 * @author Paul Ferraro
 */
public class TagMethodExpressionMarshaller implements ProtoStreamMarshaller<TagMethodExpression> {

    private static final int ATTRIBUTE_INDEX = 1;
    private static final int EXPRESSION_INDEX = 2;

    @Override
    public Class<? extends TagMethodExpression> getJavaClass() {
        return TagMethodExpression.class;
    }

    @Override
    public TagMethodExpression readFrom(ProtoStreamReader reader) throws IOException {
        String attribute = null;
        MethodExpression expression = null;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case ATTRIBUTE_INDEX:
                    attribute = reader.readString();
                    break;
                case EXPRESSION_INDEX:
                    expression = reader.readAny(MethodExpression.class);
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return new TagMethodExpression(new MockTagAttribute(attribute), expression);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, TagMethodExpression value) throws IOException {
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
