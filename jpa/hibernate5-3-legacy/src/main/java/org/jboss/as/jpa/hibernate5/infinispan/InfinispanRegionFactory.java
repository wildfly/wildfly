/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.jpa.hibernate5.infinispan;

/**
 * Infinispan-backed region factory for use with standalone (i.e. non-JPA) Hibernate applications.
 * @author Paul Ferraro
 * @author Scott Marlow
 * @deprecated Use {@link org.infinispan.hibernate.cache.v53.InfinispanRegionFactory} instead in conjunction with "hibernate.cache.infinispan.shared" set to false.
 */
@Deprecated
public class InfinispanRegionFactory extends DeprecatedInfinispanRegionFactory {
    private static final long serialVersionUID = 6526170943015350422L;

    public InfinispanRegionFactory() {
        super(false);
    }
}
