/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.annotationprocessing;

import jakarta.ejb.EJB;

public interface ReferencingBeanBInterface {

    @EJB
    ReferencedBeanInterface refencedBean = null;

}
