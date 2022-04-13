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

package org.wildfly.clustering.marshalling.protostream.reflect;

import java.lang.reflect.Field;
import java.util.function.BiFunction;

/**
 * Generic marshaller based on two non-public fields.
 * @author Paul Ferraro
 */
public class BinaryFieldMarshaller<T, F1, F2> extends BinaryMemberMarshaller<T, Field, F1, F2> {

    public BinaryFieldMarshaller(Class<? extends T> type, Class<F1> field1Type, Class<F2> field2Type, BiFunction<F1, F2, T> factory) {
        super(type, Reflect::getValue, Reflect::findField, field1Type, field2Type, factory);
    }
}
