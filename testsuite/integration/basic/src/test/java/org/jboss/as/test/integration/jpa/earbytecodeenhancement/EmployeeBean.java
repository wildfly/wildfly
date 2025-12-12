/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.earbytecodeenhancement;

import java.lang.annotation.Annotation;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;

import org.hibernate.bytecode.enhance.spi.EnhancementInfo;

/**
 * stateful session bean
 *
 * @author Stuart Douglas
 */
@Stateless
public class EmployeeBean {

    @PersistenceUnit(unitName = "mainPu")
    EntityManagerFactory entityManagerFactory;

    // AS7-2275 requires each PU reference to specify a persistence unit name, if there are
    // multiple persistence unit definitions.
    // as a workaround, specified the pu name
    @PersistenceUnit(unitName = "mainPu")
    EntityManagerFactory defaultEntityManagerFactory;

    public EntityManagerFactory getEntityManagerFactory() {
        return entityManagerFactory;
    }

    public EntityManagerFactory getDefaultEntityManagerFactory() {
        return defaultEntityManagerFactory;
    }

    // AS7-2829 bean in ejbjar should be able to access class in persistence provider
    public Class getPersistenceProviderClass(String classname) {
        Class result = null;
        try {
            result = EmployeeBean.class.getClassLoader().loadClass(classname);
        } catch (ClassNotFoundException e) {
            return null;
        }
        return result;
    }

    public boolean isEmployeeClassByteCodeEnhanced() {
        for(Annotation annotation: Employee.class.getDeclaredAnnotations())
            if(annotation instanceof EnhancementInfo) {
                return true;
            }
        return false;
    }

    public boolean isOrganisationClassByteCodeEnhanced() {
        for(Annotation annotation: Organisation.class.getDeclaredAnnotations())
            if(annotation instanceof EnhancementInfo) {
                return true;
            }
        return false;
    }
}
