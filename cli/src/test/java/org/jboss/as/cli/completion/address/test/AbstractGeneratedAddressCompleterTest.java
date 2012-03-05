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
package org.jboss.as.cli.completion.address.test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.Assert;

import org.jboss.as.cli.completion.mock.MockNode;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public abstract class AbstractGeneratedAddressCompleterTest extends AbstractAddressCompleterTest {

    protected AbstractGeneratedAddressCompleterTest() {
        super();
    }

    protected void init() {

        super.init();
        initModel();

        int prefixLevel = getPrefixLevel();
        if(prefixLevel > 0) {
            OperationRequestAddress prefix = ctx.getCurrentNodePath();
            for(int i = 1; i <= prefixLevel; ++i) {
                if(i % 2 == 0) {
                    prefix.toNode("link" + i);
                } else {
                    prefix.toNodeType("link" + i);
                }
            }
        }
    }

    protected void initModel() {

        MockNode parent = this.root;
        for (int i = 1; i <= getModelDepth(); ++i) {
            parent.addChild("last" + i);
            parent.addChild("other" + i);
            parent = parent.addChild("link" + i);
        }
    }

    protected int getModelDepth() {
        return getBufferLevel() + getPrefixLevel();
    }

    @Test
    public void testAllCandidates() {
        List<String> actual = fetchCandidates(getBufferPrefix());
        List<String> expected = getAllCandidates();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testSelectedCandidates() {
        List<String> actual = fetchCandidates(getBufferPrefix() + getSelectCandidates());
        List<String> expected = getSelectedCandidates();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testNoMatch() {
        Assert.assertEquals(Collections.emptyList(), fetchCandidates(getNoMatch()));
    }

    protected List<String> getAllCandidates() {
        int level = getPrefixLevel() + getBufferLevel();
        return Arrays.asList("last" + level, "link" + level, "other" + level);
    }

    protected List<String> getSelectedCandidates() {
        int level = getPrefixLevel() + getBufferLevel();
        return Arrays.asList("last" + level, "link" + level);
    }

    protected String getSelectCandidates() {
        return "l";
    }

    protected int getBufferLevel() {
        return 1;
    }

    private String getBufferPrefix() {
        int bufferLevel = getBufferLevel();
        if(bufferLevel < 2) {
            return "./";
        }

        StringBuilder sb = new StringBuilder("./");
        for(int i = getPrefixLevel() + 1; i < getPrefixLevel() + bufferLevel; ++i) {
            sb.append("link").append(i);
            if(i % 2 == 0) {
                sb.append('/');
            } else {
                sb.append('=');
            }
        }
        return sb.toString();
    }

    protected int getPrefixLevel() {
        return 0;
    }

    protected String getNoMatch() {
        return "nomatch";
    }

/*    private List<String> applyLevel(List<String> candidates) {
        List<String> expected = new ArrayList<String>(candidates.size());
        String levelPrefix = getBufferPrefix();
        for(String local : candidates) {
            expected.add(levelPrefix + local);
        }
        return expected;
    }
*/
    protected void assertAllCandidates(List<String> expected) {
        Assert.assertEquals(expected, getAllCandidates());
    }

    protected void assertSelectedCandidates(List<String> expected) {
        Assert.assertEquals(expected, getSelectedCandidates());
    }

    protected void assertBufferPrefix(String expected) {
        Assert.assertEquals(expected, getBufferPrefix());
    }

    protected void assertContextPrefix(String expected) {
        Assert.assertEquals(expected, ctx.getNodePathFormatter().format(ctx.getCurrentNodePath()));
    }
}
