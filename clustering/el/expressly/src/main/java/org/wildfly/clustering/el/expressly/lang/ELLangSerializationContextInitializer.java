/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.el.expressly.lang;

import org.glassfish.expressly.lang.ELSupport;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class ELLangSerializationContextInitializer extends AbstractSerializationContextInitializer {

    public ELLangSerializationContextInitializer() {
        super(ELSupport.class.getPackage());
    }

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new FunctionMarshaller());
        context.registerMarshaller(new FunctionMapperImplMarshaller());
        context.registerMarshaller(new VariableMapperImplMarshaller());
    }
}
