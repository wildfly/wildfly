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
