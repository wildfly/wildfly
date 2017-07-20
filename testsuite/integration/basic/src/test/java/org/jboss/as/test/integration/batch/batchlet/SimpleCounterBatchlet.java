/*
 * Copyright 2017 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.integration.batch.batchlet;

import javax.batch.api.AbstractBatchlet;
import javax.batch.api.BatchProperty;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Named
public class SimpleCounterBatchlet extends AbstractBatchlet {
    @Inject
    private RequestScopeCounter counter;

    @Inject
    @BatchProperty
    private int count;

    @Override
    public String process() throws Exception {
        final StringBuilder exitStatus = new StringBuilder();
        int current = 0;
        while (current < count) {
            current = counter.incrementAndGet();
            exitStatus.append(current);
            if (current < count) {
                exitStatus.append(',');
            }
        }
        return exitStatus.toString();
    }
}
