/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.marshalling;

import java.util.List;

import org.infinispan.commons.marshall.Marshaller;
import org.jboss.modules.ModuleLoader;
import org.wildfly.clustering.marshalling.ByteBufferMarshaller;

/**
 * @author Paul Ferraro
 */
public interface MarshallerFactory {

    ByteBufferMarshaller createByteBufferMarshaller(ModuleLoader moduleLoader, List<ClassLoader> loaders);

    Marshaller createUserMarshaller(ModuleLoader moduleLoader, List<ClassLoader> loaders);
}
