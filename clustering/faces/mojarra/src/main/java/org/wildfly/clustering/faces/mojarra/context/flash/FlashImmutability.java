/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.faces.mojarra.context.flash;

import java.util.Set;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.immutable.Immutability;

import com.sun.faces.context.flash.ELFlash;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(Immutability.class)
public class FlashImmutability implements Immutability {

    private final Immutability immutability = Immutability.instanceOf(Set.of(findClass("com.sun.faces.context.flash.SessionHelper")));

    private static Class<?> findClass(String className) {
        try {
            return ELFlash.class.getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public boolean test(Object object) {
        return this.immutability.equals(object);
    }
}
