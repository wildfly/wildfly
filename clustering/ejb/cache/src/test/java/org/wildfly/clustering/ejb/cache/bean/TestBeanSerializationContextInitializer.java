/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ejb.cache.bean;

import java.util.List;

import org.infinispan.protostream.SerializationContextInitializer;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.ejb.client.EJBClientSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.CompositeSerializationContextInitializer;

/**
 * A {@link SerializationContextInitializer} that registers the requisite marshallers for use by this module's marshalling tests.
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class TestBeanSerializationContextInitializer extends CompositeSerializationContextInitializer {

    public TestBeanSerializationContextInitializer() {
        super(List.of(new EJBClientSerializationContextInitializer(), new BeanSerializationContextInitializer()));
    }
}
