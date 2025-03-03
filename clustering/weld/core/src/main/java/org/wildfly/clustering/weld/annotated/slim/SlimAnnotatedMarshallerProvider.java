/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.annotated.slim;

import org.jboss.weld.annotated.slim.AnnotatedTypeIdentifier;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class SlimAnnotatedMarshallerProvider extends AbstractSerializationContextInitializer {

    public SlimAnnotatedMarshallerProvider() {
        super(AnnotatedTypeIdentifier.class.getPackage());
    }

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new AnnotatedTypeIdentifierMarshaller());
    }
}
