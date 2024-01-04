/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.mappedname;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * User: jpai
 */
@Stateless
public class MappedNameBean {

    @Resource(mappedName = "java:comp/env/ResourceFromWebXml")
    private Object resourceByMappedName;

    @Resource(lookup = "java:comp/env/ResourceFromWebXml")
    private Object resourceByLookupName;

    @EJB(lookup = "java:module/MappedNameBean")
    private MappedNameBean selfByLookupName;

    @EJB(mappedName = "java:module/MappedNameBean")
    private MappedNameBean selfByMappedName;

    public boolean isResourceWithMappedNameInjected() {
        return this.resourceByMappedName != null;
    }

    public boolean isResourceWithLookupNameInjected() {
        return this.resourceByLookupName != null;
    }

    public boolean isEJBWithLookupNameInjected() {
        return this.selfByLookupName != null;
    }

    public boolean isEJBWithMappedNameInjected() {
        return this.selfByMappedName != null;
    }
}
