/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2023 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.integration.ee.injection.support.jpa.cdi;

import jakarta.enterprise.context.RequestScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;

/**
 * Manages the user repository.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RequestScoped
@Transactional
public class UserRegistry {
    @PersistenceContext(unitName = "test")
    private EntityManager em;

    /**
     * Finds the user given the id.
     *
     * @param id the user id
     *
     * @return the user or {@code null} if not found
     */
    public User getUserById(final long id) {
        return em.find(User.class, id);
    }

    /**
     * Updates, or adds if missing, the user.
     *
     * @param user the user to update
     *
     * @return the updated user
     */
    public User update(@NotNull final User user) {
        return em.merge(user);
    }

    /**
     * Adds a user to the repository.
     *
     * @param user the user to add
     *
     * @return the user
     */
    public User add(@NotNull final User user) {
        em.persist(user);
        return user;
    }

    /**
     * Deletes the user, if it exists, from the repository.
     *
     * @param id the user id
     *
     * @return the deleted user
     */
    public User remove(final long id) {
        final User user = em.find(User.class, id);
        if (user != null) {
            em.remove(user);
        }
        return user;
    }
}
