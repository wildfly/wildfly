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
public interface AnnoAdminObject extends Referenceable, Serializable {

    /**
     * Set first
     *
     * @param first The value
     */
    void setFirst(Long first);

    /**
     * Get first
     *
     * @return The value
     */
    Long getFirst();

    /**
     * Set second
     *
     * @param second The value
     */
    void setSecond(Boolean second);

    /**
     * Get second
     *
     * @return The value
     */
    Boolean getSecond();

}
