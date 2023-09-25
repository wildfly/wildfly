/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jipijapa.plugin.spi;

import jakarta.persistence.EntityManagerFactory;

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
     * Allows passing in a Jakarta EE ValidatorFactory (delayed from constructing the builder, AKA phase 2) to be used
     * in building the EntityManagerFactory
     *
     * @param validatorFactory The ValidatorFactory
     *
     * @return {@code this}, for method chaining
     */
    EntityManagerFactoryBuilder withValidatorFactory(Object validatorFactory);

}
