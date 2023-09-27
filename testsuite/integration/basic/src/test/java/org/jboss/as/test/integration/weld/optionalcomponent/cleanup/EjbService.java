/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.optionalcomponent.cleanup;

import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

/**
 *
 * @author <a href="mailto:manovotn@redhat.com">Matej Novotny</a>
 */
@Singleton
@Startup
public class EjbService {
}
