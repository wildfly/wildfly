/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.deployment.rar.configproperty;

import java.io.Serializable;
import jakarta.resource.Referenceable;

/**
 * ConfigPropertyAdminObjectInterface
 *
 * @version $Revision: $
 */
public interface ConfigPropertyAdminObjectInterface extends Referenceable, Serializable {
    /**
     * Set property
     *
     * @param property The value
     */
    void setProperty(String property);

    /**
     * Get property
     *
     * @return The value
     */
    String getProperty();
}
