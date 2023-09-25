/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.discovery.vetoed;

import jakarta.inject.Inject;

import org.jboss.as.weld.discovery.AlphaImpl;
import org.jboss.as.weld.discovery.InnerClasses.InnerInterface;

public class Bravo extends AlphaImpl implements InnerInterface {

    @Inject
    Long charlie;

}
