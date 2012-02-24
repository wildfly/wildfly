/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.management.api.core;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;

/**
 * Test for functionality added with AS7-2139.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ResolveExpressionTestCase extends AbstractMgmtTestBase {

    @Deployment
    public static Archive<?> getDeployment() {
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "dummy.jar");
        ja.addClass(ResolveExpressionTestCase.class);
        return ja;
    }

    @Test
    public void testResolveExpression() throws Exception  {
        ModelNode op = createOpNode(null, "resolve-expression");
        op.get("expression").set("${file.separator}");

        Assert.assertEquals(System.getProperty("file.separator"), executeOperation(op).asString());
    }

    @Test
    public void testNonExpression() throws Exception  {
        ModelNode op = createOpNode(null, "resolve-expression");
        op.get("expression").set("hello");

        Assert.assertEquals("hello", executeOperation(op).asString());
    }

    @Test
    public void testUndefined() throws Exception  {
        ModelNode op = createOpNode(null, "resolve-expression");

        Assert.assertFalse(executeOperation(op).isDefined());

        op.get("expression");

        Assert.assertFalse(executeOperation(op).isDefined());
    }

    @Test
    public void testUnresolvableExpression() throws Exception  {
        ModelNode op = createOpNode(null, "resolve-expression");
        op.get("expression").set("${unresolvable}");

        ModelNode response = executeOperation(op, false);
        Assert.assertFalse("Management operation " + op.asString() + " succeeded: " + response.toString(),
                "success".equals(response.get("outcome").asString()));
    }
}
