/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.rar;

import java.io.Serializable;
import jakarta.resource.Referenceable;

/**
 * MultipleAdminObject2
 *
 * @version $Revision: $
 */
public interface MultipleAdminObject2 extends Referenceable, Serializable {

    /**
     * Set name
     *
     * @param name The value
     */
    void setName(String name);

    /**
     * Get name
     *
     * @return The value
     */
    String getName();


}
