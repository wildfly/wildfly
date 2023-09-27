/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.faces.mojarra.context.flash;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.PrivilegedAction;

import jakarta.servlet.http.HttpSessionActivationListener;

import org.wildfly.clustering.marshalling.protostream.ValueMarshaller;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author Paul Ferraro
 */
public class SessionHelperMarshaller extends ValueMarshaller<HttpSessionActivationListener> {

    public SessionHelperMarshaller() {
        super(WildFlySecurityManager.doUnchecked(new PrivilegedAction<HttpSessionActivationListener>() {
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
        }));
    }
}
