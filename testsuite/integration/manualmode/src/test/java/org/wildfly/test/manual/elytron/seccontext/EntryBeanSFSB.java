/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.manual.elytron.seccontext;

import jakarta.ejb.Stateful;

/**
 * Stateful version of the {@link EntryBean}.
 *
 * @author Josef Cacek
 */
@Stateful
public class EntryBeanSFSB extends EntryBean implements Entry {

}
