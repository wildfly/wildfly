/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.container;

import java.io.Serializable;
import java.util.UUID;

/**
 * Uniquely identifies an ExtendedEntityManager instance.
 *
 * @author Scott Marlow
 */
public class ExtendedEntityManagerKey implements Serializable {
    private static final long serialVersionUID = 135790L;
    private final String ID = UUID.randomUUID().toString();

    /**
     * generates a new unique ExtendedEntityManagerID
     * @return unique ExtendedEntityManagerID
     */
    public static ExtendedEntityManagerKey extendedEntityManagerID() {
        return new ExtendedEntityManagerKey();
    }

    @Override
    public String toString() {
        return "ExtendedEntityManagerKey{" +
            "ID='" + ID + '\'' +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || ! (o instanceof ExtendedEntityManagerKey))
            return false;

        ExtendedEntityManagerKey that = (ExtendedEntityManagerKey) o;
        if (ID != null ? !ID.equals(that.getKey()) : that.getKey() != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return ID != null ? ID.hashCode() : 0;
    }


    public String getKey() {
        return ID;
    }


}
