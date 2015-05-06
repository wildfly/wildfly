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

package org.jipijapa.management.spi;

import javax.persistence.EntityManagerFactory;

/**
 * EntityManagerFactoryAccess
 *
 * @author Scott Marlow
 */
public interface EntityManagerFactoryAccess {
    /**
     * returns the entity manager factory that statistics should be obtained for.
     *
     * @throws IllegalStateException if scopedPersistenceUnitName is not found
     *
     * @param scopedPersistenceUnitName is persistence unit name scoped to the current platform
     *
     * @return EntityManagerFactory
     */
    EntityManagerFactory entityManagerFactory(String scopedPersistenceUnitName) throws IllegalStateException;

}
