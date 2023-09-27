/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.beanvalidation.ra;

import java.io.Serializable;
import jakarta.resource.Referenceable;

/**
 * Admin object
 *
 * @author <a href="mailto:vrastsel@redhat.com">Vladimir Rastseluev</a>
 */
public interface ValidAdminObjectInterface extends Referenceable, Serializable {
    /**
     * Set property
     *
     * @param property The value
     */
    void setAoProperty(String property);

    /**
     * Get property
     *
     * @return The value
     */
    String getAoProperty();
}
