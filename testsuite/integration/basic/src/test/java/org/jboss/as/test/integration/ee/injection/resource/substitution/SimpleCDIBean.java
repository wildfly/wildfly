/*
 *
 *  * JBoss, Home of Professional Open Source.
 *  * Copyright $year Red Hat, Inc., and individual contributors
 *  * as indicated by the @author tags.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.jboss.as.test.integration.ee.injection.resource.substitution;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.enterprise.context.RequestScoped;

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
