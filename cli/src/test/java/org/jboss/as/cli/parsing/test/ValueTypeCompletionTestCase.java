/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.cli.parsing.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jboss.as.cli.impl.ValueTypeCompleter;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class ValueTypeCompletionTestCase {

    private static final String loginModulesDescr = "{" +
            "\"type\" => LIST," +
            "\"description\" => \"List of authentication modules\"," +
            "\"expressions-allowed\" => false," +
            "\"required\" => true," +
            "\"nillable\" => false," +
            "\"value-type\" => {" +
            "     \"code\" => {" +
            "        \"description\" => \"Class name of the module to be instantiated.\"," +
            "        \"type\" => BOOLEAN," +
            "        \"nillable\" => false" +
            "     }," +
            "    \"flag\" => {" +
            "        \"description\" => \"The flag controls how the module participates in the overall procedure.\"," +
            "        \"type\" => STRING," +
            "        \"nillable\" => false," +
            "        \"allowed\" => [" +
            "            \"required\"," +
            "            \"requisite\"," +
            "            \"sufficient\"," +
            "            \"optional\"" +
            "        ]" +
            "    }," +
            "    \"module\" => {" +
            "        \"type\" => STRING," +
            "        \"nillable\" => true," +
            "        \"description\" => \"Name of JBoss Module where the login module code is located.\"" +
            "    }," +
            "    \"module-options\" => {" +
            "        \"description\" => \"List of module options containing a name/value pair.\"," +
            "        \"type\" => OBJECT," +
            "        \"value-type\" => STRING," +
            "        \"nillable\" => true" +
            "    }," +
            "    \"aa\" => {" +
            "        \"description\" => \"smth\"," +
            "        \"type\" => OBJECT," +
            "        \"value-type\" => {" +
            "            \"ab1\" => {" +
            "                \"description\" => \"smth\"," +
            "                \"type\" => STRING," +
            "            }," +
            "            \"ab2\" => {" +
            "                \"description\" => \"smth\"," +
            "                \"type\" => STRING," +
            "            }," +
            "            \"ac1\" => {" +
            "                \"description\" => \"smth\"," +
            "                \"type\" => BOOLEAN," +
            "            }" +
            "        }" +
            "    }," +
            "    \"bb\" => {" +
            "        \"description\" => \"smth\"," +
            "        \"type\" => LIST," +
            "        \"value-type\" => {" +
            "            \"bb1\" => {" +
            "                \"description\" => \"smth\"," +
            "                \"type\" => STRING," +
            "            }," +
            "            \"bb2\" => {" +
            "                \"description\" => \"smth\"," +
            "                \"type\" => STRING," +
            "            }," +
            "            \"bc1\" => {" +
            "                \"description\" => \"smth\"," +
            "                \"type\" => STRING," +
            "            }" +
            "        }" +
            "    }" +
            "}" +
        "}";

    @Test
    public void testLoginModules() throws Exception {
        final ModelNode propDescr = ModelNode.fromString(loginModulesDescr);
        assertTrue(propDescr.isDefined());

        final ValueTypeCompleter completer = new ValueTypeCompleter(propDescr);
        final List<String> candidates = new ArrayList<String>();

        int i;
        i = completer.complete(null, "", 0, candidates);
        assertEquals(Collections.singletonList("["), candidates);
        assertEquals(0, i);

        candidates.clear();
        i = completer.complete(null, "[", 0, candidates);
        assertEquals(Arrays.asList(new String[]{"aa", "bb", "code", "flag", "module", "module-options"}), candidates);
        assertEquals(1, i);

        candidates.clear();
        i = completer.complete(null, "f", 0, candidates);
        assertEquals(Collections.singletonList("flag"), candidates);
        assertEquals(0, i);


        candidates.clear();
        i = completer.complete(null, "m", 0, candidates);
        assertEquals(Arrays.asList(new String[]{"module", "module-options"}), candidates);
        assertEquals(0, i);

        candidates.clear();
        i = completer.complete(null, "module", 0, candidates);
        assertEquals(Arrays.asList(new String[]{"module", "module-options"}), candidates);
        assertEquals(0, i);

        candidates.clear();
        i = completer.complete(null, "module=", 0, candidates);
        assertEquals(Collections.emptyList(), candidates);
        assertEquals(-1 /*7*/, i);

        candidates.clear();
        i = completer.complete(null, "module=m", 0, candidates);
        assertEquals(Collections.emptyList(), candidates);
        assertEquals(-1 /*7*/, i);

        candidates.clear();
        i = completer.complete(null, "flag = ", 0, candidates);
        assertEquals(Arrays.asList(new String[]{"optional", "required", "requisite", "sufficient"}), candidates);
        assertEquals(6, i);

        candidates.clear();
        i = completer.complete(null, "flag= s", 0, candidates);
        assertEquals(Collections.singletonList("sufficient"), candidates);
        assertEquals(6, i);

        candidates.clear();
        i = completer.complete(null, "flag=requi", 0, candidates);
        assertEquals(Arrays.asList(new String[]{"required", "requisite"}), candidates);
        assertEquals(5, i);

        candidates.clear();
        i = completer.complete(null, "code=", 0, candidates);
        assertEquals(Arrays.asList(new String[]{"false", "true"}), candidates);
        assertEquals(/*-1*/5, i);

        candidates.clear();
        i = completer.complete(null, "code=t", 0, candidates);
        assertEquals(Collections.singletonList("true"), candidates);
        assertEquals(/*-1*/5, i);

        candidates.clear();
        i = completer.complete(null, "code=Main", 0, candidates);
        assertEquals(Collections.emptyList(), candidates);
        assertEquals(-1 /*5*/, i);

        candidates.clear();
        i = completer.complete(null, "code=Main,", 0, candidates);
        assertEquals(Arrays.asList(new String[]{"aa", "bb", "flag", "module", "module-options"}), candidates);
        assertEquals(10, i);

        candidates.clear();
        i = completer.complete(null, "code=Main,w", 0, candidates);
        assertEquals(Collections.emptyList(), candidates);
        assertEquals(-1 /*10*/, i);

        candidates.clear();
        i = completer.complete(null, "code=Main,module", 0, candidates);
        assertEquals(Arrays.asList(new String[]{"module", "module-options"}), candidates);
        assertEquals(10, i);

        candidates.clear();
        i = completer.complete(null, "code=Main,fl", 0, candidates);
        assertEquals(Arrays.asList(new String[]{"flag"}), candidates);
        assertEquals(10, i);

        candidates.clear();
        i = completer.complete(null, "code=Main,flag = ", 0, candidates);
        assertEquals(Arrays.asList(new String[]{"optional", "required", "requisite", "sufficient"}), candidates);
        assertEquals(16, i);

        candidates.clear();
        i = completer.complete(null, "code=Main,flag = requi", 0, candidates);
        assertEquals(Arrays.asList(new String[]{"required", "requisite"}), candidates);
        assertEquals(17, i);

        candidates.clear();
        i = completer.complete(null, "code=Main,flag = required", 0, candidates);
        assertEquals(Arrays.asList(new String[]{"required"}), candidates);
        assertEquals(17, i);

        candidates.clear();
        i = completer.complete(null, "code=Main,flag = required,", 0, candidates);
        assertEquals(Arrays.asList(new String[]{"aa", "bb", "module", "module-options"}), candidates);
        assertEquals(26, i);

        candidates.clear();
        i = completer.complete(null, "code=Main,flag = required,aa=", 0, candidates);
        assertEquals(Arrays.asList(new String[]{"{"}), candidates);
        assertEquals(29, i);

        candidates.clear();
        i = completer.complete(null, "code=Main,flag = required,aa={", 0, candidates);
        assertEquals(Arrays.asList(new String[]{"ab1", "ab2", "ac1"}), candidates);
        assertEquals(30, i);

        candidates.clear();
        i = completer.complete(null, "code=Main,flag = required,aa={ab", 0, candidates);
        assertEquals(Arrays.asList(new String[]{"ab1", "ab2"}), candidates);
        assertEquals(30, i);

        candidates.clear();
        i = completer.complete(null, "code=Main,flag = required,aa={ab1=1,", 0, candidates);
        assertEquals(Arrays.asList(new String[]{"ab2", "ac1"}), candidates);
        assertEquals(36, i);

        candidates.clear();
        i = completer.complete(null, "code=Main,flag = required,aa={ab1=1,a", 0, candidates);
        assertEquals(Arrays.asList(new String[]{"ab2", "ac1"}), candidates);
        assertEquals(36, i);

        candidates.clear();
        i = completer.complete(null, "code=Main,flag = required,aa={ab1=1,ac", 0, candidates);
        assertEquals(Arrays.asList(new String[]{"ac1"}), candidates);
        assertEquals(36, i);

        candidates.clear();
        i = completer.complete(null, "code=Main,flag = required,aa={ab1=1,ac1=", 0, candidates);
        assertEquals(Arrays.asList(new String[]{"false", "true"}), candidates);
        assertEquals(/*36*/ 40, i);

        candidates.clear();
        i = completer.complete(null, "code=Main,flag = required,aa={ab1=1,ac1=s", 0, candidates);
        assertEquals(Collections.emptyList(), candidates);
        assertEquals(-1, i);

        candidates.clear();
        i = completer.complete(null, "code=Main,flag = required,aa={ab1=1,ac1=f", 0, candidates);
        assertEquals(Collections.singletonList("false"), candidates);
        assertEquals(40, i);

        //assertEquals(Arrays.asList(new String[]{","}), valueTypeHandler.getCandidates(valueType, "code=Main,flag = required,aa={ab1=1,ac1=2}"));
        candidates.clear();
        i = completer.complete(null, "code=Main,flag = required,aa={ab1=1,ac1=false", 0, candidates);
        assertEquals(Collections.singletonList("false"), candidates);
        assertEquals(40, i);

        candidates.clear();
        i = completer.complete(null, "code=Main,flag = required,aa={ab1=1,ac1=2,", 0, candidates);
        assertEquals(Arrays.asList(new String[]{"ab2"}), candidates);
        assertEquals(42, i);

        candidates.clear();
        i = completer.complete(null, "code=Main,flag = required,aa={ab1=1,ac1=2},", 0, candidates);
        assertEquals(Arrays.asList(new String[]{"bb", "module", "module-options"}), candidates);
        assertEquals(43, i);

        candidates.clear();
        i = completer.complete(null, "code=Main,flag = required,aa={ab1=1,ac1=2},bb=", 0, candidates);
        assertEquals(Arrays.asList(new String[]{"["}), candidates);
        assertEquals(46, i);

        candidates.clear();
        i = completer.complete(null, "code=Main,flag = required,aa={ab1=1,ac1=2},bb=[", 0, candidates);
        assertEquals(Arrays.asList(new String[]{"bb1", "bb2", "bc1"}), candidates);
        assertEquals(47, i);
    }
}
