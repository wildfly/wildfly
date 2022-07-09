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

import java.lang.reflect.Method;
import java.util.function.BiFunction;

/**
 * Generic marshaller based on two non-public accessor methods.
 * @author Paul Ferraro
 */
public class BinaryMethodMarshaller<T, M1, M2> extends BinaryMemberMarshaller<T, Method, M1, M2> {

    public BinaryMethodMarshaller(Class<? extends T> type, Class<M1> member1Type, Class<M2> member2Type, BiFunction<M1, M2, T> factory) {
        super(type, Reflect::invoke, Reflect::findMethod, member1Type, member2Type, factory);
    }
}