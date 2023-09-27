/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.scheduler;

import java.util.function.UnaryOperator;

/**
 * Unit test for {@link LinkedScheduledEntries}
 * @author Paul Ferraro
 */
public class LinkedScheduledEntriesTestCase extends AbstractScheduledEntriesTestCase {

    public LinkedScheduledEntriesTestCase() {
        super(new LinkedScheduledEntries<>(), UnaryOperator.identity());
    }
}
