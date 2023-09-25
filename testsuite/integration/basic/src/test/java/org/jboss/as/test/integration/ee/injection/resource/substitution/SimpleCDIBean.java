/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.resource.substitution;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class SimpleCDIBean {

    @Resource(name = "${resource.name}")
    private String resourceByName;

    @Resource(lookup = "${resource.lookup}")
    private Object resourceByLookupName;

    @Resource(mappedName = "${resource.mappedname}")
    private Object resourceByMappedName;

    @PostConstruct
    public void postConstruct() {
        if (resourceByName == null) {
            throw new IllegalStateException("resourceByName");
        }
        if (resourceByLookupName == null) {
            throw new IllegalStateException("resourceByLookupName");
        }
        if (resourceByMappedName == null) {
            throw new IllegalStateException("resourceByMappedName");
        }
    }

    public boolean isResourceWithNameInjected() {
        return this.resourceByName != null;
    }

    public boolean isResourceWithMappedNameInjected() {
        return this.resourceByMappedName != null;
    }

    public boolean isResourceWithLookupNameInjected() {
        return this.resourceByLookupName != null;
    }
}
