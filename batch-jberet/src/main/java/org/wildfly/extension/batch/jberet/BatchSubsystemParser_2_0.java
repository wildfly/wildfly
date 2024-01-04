/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.batch.jberet;

import java.util.Collections;
import java.util.List;
import javax.xml.stream.XMLStreamConstants;

import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class BatchSubsystemParser_2_0 extends BatchSubsystemParser_1_0 implements XMLStreamConstants, XMLElementReader<List<ModelNode>> {

    public BatchSubsystemParser_2_0() {
        super(Collections.singletonMap(Element.SECURITY_DOMAIN, BatchSubsystemDefinition.SECURITY_DOMAIN));
    }
}