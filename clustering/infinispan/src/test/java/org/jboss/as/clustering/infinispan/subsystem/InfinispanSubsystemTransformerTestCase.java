/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
* as indicated by the @author tags. See the copyright.txt file in the
* distribution for a full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.junit.Test;

import java.io.IOException;

/**
 * @author <a href="tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */

public class InfinispanSubsystemTransformerTestCase extends AbstractSubsystemBaseTest {


    public InfinispanSubsystemTransformerTestCase() {
        super(InfinispanExtension.SUBSYSTEM_NAME, new InfinispanExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("infinispan-transformer_1_4.xml");
    }

    @Test
    public void testConverter() throws Exception {
        String dmr = readResource("infinispan-1.3.dmr");
        ModelNode expected = ModelNode.fromString(dmr);

        testConverter(expected, 1, 3);
    }

    private static ModelNode cleanUndefined(ModelNode model) {
        if (model.isDefined()) {
            for (Property p : model.asPropertyList()) {
                if (p.getValue().getType() == ModelType.OBJECT) {
                    model.get(p.getName()).set(cleanUndefined(p.getValue()));
                } else {
                    if (!p.getValue().isDefined()) {
                        model.remove(p.getName());
                    }
                }
            }
        }
        return model;
    }
}
