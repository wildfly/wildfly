/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.config;

/**
 * Represents the Jakarta Persistence per application deployment settings read from jboss-all.xml
 *
* @author Scott Marlow
*/
public class JPADeploymentSettings {

    private ExtendedPersistenceInheritance extendedPersistenceInheritanceType = ExtendedPersistenceInheritance.SHALLOW;

    public ExtendedPersistenceInheritance getExtendedPersistenceInheritanceType() {
        return extendedPersistenceInheritanceType;
    }

    public void setExtendedPersistenceInheritanceType(ExtendedPersistenceInheritance extendedPersistenceInheritanceType) {
        this.extendedPersistenceInheritanceType = extendedPersistenceInheritanceType;
    }

}
