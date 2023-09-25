/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.beanvalidation.hibernate.scriptassert;

import org.hibernate.validator.constraints.ScriptAssert;

/**
 *
 * @author Stuart Douglas
 */
@ScriptAssert(lang = "javascript", script = "false")
public class ScriptAssertBean {
}
