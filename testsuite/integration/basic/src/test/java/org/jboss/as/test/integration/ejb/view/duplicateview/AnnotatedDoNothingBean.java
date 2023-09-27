/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.view.duplicateview;

import jakarta.ejb.Local;
import jakarta.ejb.Stateless;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@Stateless
@Local(DoNothing.class)
public class AnnotatedDoNothingBean extends DoNothingBean {
}
