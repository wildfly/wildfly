/*
 * Copyright 2016 Red Hat, Inc.
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

package org.wildfly.extension.batch.jberet;

import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;

import javax.xml.stream.XMLStreamConstants;
import java.util.List;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class BatchSubsystemParser_3_0 extends BatchSubsystemParser_2_0 implements XMLStreamConstants, XMLElementReader<List<ModelNode>> {

    public BatchSubsystemParser_3_0() {
        super();
    }
}