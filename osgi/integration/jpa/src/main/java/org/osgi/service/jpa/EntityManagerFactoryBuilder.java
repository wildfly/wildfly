/*
 * Copyright (c) OSGi Alliance (2009, 2010). All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.osgi.service.jpa;

import java.util.Map;

import javax.persistence.EntityManagerFactory;

/**
 * This service interface offers JPA clients the ability to create instances of EntityManagerFactory for a given named
 * persistence unit. A service instance will be created for each named persistence unit and can be filtered by comparing the
 * value of the osgi.unit.name property containing the persistence unit name.
 *
 * This service is used specifically when the caller wants to pass in factory-scoped properties as arguments. If no properties
 * are being used in the creation of the EntityManagerFactory then the basic EntityManagerFactory service should be used.
 */
public interface EntityManagerFactoryBuilder {

    /**
     * The name of the persistence unit.
     */
    String JPA_UNIT_NAME = "osgi.unit.name";

    /**
     * The version of the persistence unit bundle.
     */
    String JPA_UNIT_VERSION = "osgi.unit.version";

    /**
     * The class name of the provider that registered the service and implements the JPA javax.persistence.PersistenceProvider
     * interface.
     */
    String JPA_UNIT_PROVIDER = "osgi.unit.provider";

    /**
     * Return an EntityManagerFactory instance configured according to the properties defined in the corresponding persistence
     * descriptor, as well as the properties passed into the method.
     *
     * @param props Properties to be used, in addition to those in the persistence descriptor, for configuring the
     *        EntityManagerFactory for the persistence unit.
     *
     * @return An EntityManagerFactory for the persistence unit associated with this service. Must not be null.
     */
    EntityManagerFactory createEntityManagerFactory(Map<String, Object> props);
}
