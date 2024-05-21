/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.faces.mojarra.context.flash;

import java.util.Set;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.immutable.Immutability;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(Immutability.class)
public class FlashImmutability implements Immutability {

    private final Immutability immutability = Immutability.instanceOf(Set.of(Reflect.getSessionHelperClass()));

    @Override
    public boolean test(Object object) {
        return this.immutability.equals(object);
    }
}
