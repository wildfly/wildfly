/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

@NamedQuery(name = "allMouse",
        query = "select m from ApplicationServer m")
package org.hibernate.jpa.test.pack.war;

import org.hibernate.annotations.NamedQuery;