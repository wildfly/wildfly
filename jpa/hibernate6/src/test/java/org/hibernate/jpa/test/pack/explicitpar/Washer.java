/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hibernate.jpa.test.pack.explicitpar;

import jakarta.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Washer {
    //No @id so picking it up should fail
}
