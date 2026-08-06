/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.faces.mojarra.context.flash;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import com.sun.faces.context.flash.ELFlash;

import jakarta.servlet.http.HttpSessionActivationListener;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.function.Supplier;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class ContextFlashSerializationContextInitializer extends AbstractSerializationContextInitializer {

    public ContextFlashSerializationContextInitializer() {
        super(ELFlash.class.getPackage());
    }

    @Override
    public void registerMarshallers(SerializationContext context) {
        try {
            // *sigh* SessionHelper is package protected
            Class<? extends HttpSessionActivationListener> targetClass = ELFlash.class.getClassLoader().loadClass("com.sun.faces.context.flash.SessionHelper").asSubclass(HttpSessionActivationListener.class);
            MethodHandle handle = MethodHandles.privateLookupIn(targetClass, MethodHandles.lookup()).findConstructor(targetClass, MethodType.methodType(void.class));
            HttpSessionActivationListener listener = Supplier.<HttpSessionActivationListener>invoke(handle).get();
            // Set passivated flag
            listener.sessionDidActivate(null);
            context.registerMarshaller(ProtoStreamMarshaller.of(listener));
        } catch (NoSuchMethodException | IllegalAccessException | ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }
}
