/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.jakarta.data.lib;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.Repository;

/** Repository for manipulating entities representing people who might be hired for something. */
@Repository
public interface Recruiter extends CrudRepository<Person, Long> {

    default Person recruit(String name, LocalDate birthDate) {
        Optional<Person> optional = find(name, birthDate);
        if (optional.isEmpty()) {
            Person person = new Person(name, birthDate);
            return insert(person);
        }
        return optional.get();
    }

    @Find
    Optional<Person> find(String name, LocalDate birthdate);

    @Find
    Optional<Person> find(String name);
}