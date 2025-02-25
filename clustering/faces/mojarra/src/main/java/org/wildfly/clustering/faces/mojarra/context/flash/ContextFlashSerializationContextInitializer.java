/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.faces.mojarra.context.flash;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.PrivilegedAction;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;
import org.wildfly.security.manager.WildFlySecurityManager;

import com.sun.faces.context.flash.ELFlash;

import jakarta.servlet.http.HttpSessionActivationListener;

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
        context.registerMarshaller(ProtoStreamMarshaller.of(WildFlySecurityManager.doUnchecked(new PrivilegedAction<HttpSessionActivationListener>() {
            @Override
            public HttpSessionActivationListener run() {
                try {
                    // *sigh* SessionHelper is package protected
                    Class<? extends HttpSessionActivationListener> targetClass = Reflect.getSessionHelperClass();
                    Constructor<? extends HttpSessionActivationListener> constructor = targetClass.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    HttpSessionActivationListener listener = constructor.newInstance();
                    // Set passivated flag
                    listener.sessionWillPassivate(null);
                    return listener;
                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new IllegalStateException(e);
                }
            }
        })));
    }
}
