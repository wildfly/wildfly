/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.weld.extensions.buildcompatible.subsystem;

import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.Discovery;
import jakarta.enterprise.inject.build.compatible.spi.ScannedClasses;

public class RegisteredExtension implements BuildCompatibleExtension {

    @Discovery
    public void discovery(ScannedClasses sc) {
        sc.add(RegisteredBean.class.getName());
    }
}
