/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.faces.mojarra.context.flash;

import java.util.Set;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.ee.Immutability;
import org.wildfly.clustering.ee.immutable.InstanceOfImmutability;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(Immutability.class)
public class FlashImmutability extends InstanceOfImmutability {

    public FlashImmutability() {
        super(Set.of(Reflect.getSessionHelperClass()));
    }
}
