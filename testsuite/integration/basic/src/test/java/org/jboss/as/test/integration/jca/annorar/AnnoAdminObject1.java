/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.annorar;

import java.io.Serializable;
import jakarta.resource.Referenceable;

/**
 * AnnoAdminObject
 *
 * @version $Revision: $
 */
public interface AnnoAdminObject1 extends Referenceable, Serializable {

    /**
     * Set first
     *
     * @param first The value
     */
    void setFirst(Float first);

    /**
     * Get first
     *
     * @return The value
     */
    Float getFirst();

    /**
     * Set second
     *
     * @param second The value
     */
    void setSecond(String second);

    /**
     * Get second
     *
     * @return The value
     */
    String getSecond();

}
