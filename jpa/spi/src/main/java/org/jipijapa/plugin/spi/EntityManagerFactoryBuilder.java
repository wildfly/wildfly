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

package org.jipijapa.plugin.spi;

import javax.persistence.EntityManagerFactory;

/**
 * EntityManagerFactoryBuilder is based on org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder.
 * Represents a 2-phase JPA bootstrap process for building an EntityManagerFactory.
 *
 * @author Scott Marlow
 * @author Steve Ebersole
 */
public interface EntityManagerFactoryBuilder {
    /**
     * Build {@link EntityManagerFactory} instance
     *
     * @return The built {@link EntityManagerFactory}
     */
    EntityManagerFactory build();

    /**
     * Cancel the building processing. This is used to signal the builder to release any resources in the case of
     * something having gone wrong during the bootstrap process
     */
    void cancel();

    /**
     * Allows passing in a Java EE ValidatorFactory (delayed from constructing the builder, AKA phase 2) to be used
     * in building the EntityManagerFactory
     *
     * @param validatorFactory The ValidatorFactory
     *
     * @return {@code this}, for method chaining
     */
    EntityManagerFactoryBuilder withValidatorFactory(Object validatorFactory);

}
