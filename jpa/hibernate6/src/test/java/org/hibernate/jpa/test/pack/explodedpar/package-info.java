/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

@NamedQuery(name = "allCarpet", query = "select c from Carpet c")
package org.hibernate.jpa.test.pack.explodedpar;

import org.hibernate.annotations.NamedQuery;