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
package org.jboss.as.test.integration.management.cli;

import java.io.IOException;
import java.util.Map;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.as.test.integration.management.util.CLIOpResult;
import org.junit.AfterClass;
import static org.junit.Assert.*;

import static org.hamcrest.CoreMatchers.*;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2013 Red Hat, inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class LogFilterTestCase extends AbstractCliTestBase {

    @BeforeClass
    public static void before() throws Exception {
        AbstractCliTestBase.initCLI();
    }

    @AfterClass
    public static void after() throws Exception {
        cli.sendLine("/subsystem=logging/console-handler=CONSOLE:undefine-attribute(name=filter)");
        AbstractCliTestBase.closeCLI();
    }

    @Test
    public void addLoggingFilter() throws IOException {
        cli.sendLine("cd subsystem=logging/console-handler=CONSOLE");
        cli.sendLine(":write-attribute(name=filter, value={replace => {\"pattern\" => \"JBAS\",\"replacement\" => \"DUMMY\",\"replace-all\" => true}})");
        CLIOpResult result = cli.readAllAsOpResult();
        assertThat(result, is(notNullValue()));
        assertThat(result.getFromResponse(OUTCOME).toString(), is("success"));
        cli.sendLine(":read-resource");
        result = cli.readAllAsOpResult();
        assertThat(result, is(notNullValue()));
        assertThat(result.getFromResponse(OUTCOME).toString(), is("success"));
        Object filterSpecAttr = result.getResultAsMap().get("filter-spec");
        assertThat(filterSpecAttr, is(notNullValue()));
        assertThat(filterSpecAttr.toString(), is("substituteAll(\"JBAS\",\"DUMMY\")"));
        Map filter = (Map) result.getResultAsMap().get("filter");
        assertThat(filter, is(notNullValue()));
        Map replace = (Map) filter.get("replace");
        assertThat(replace, is(notNullValue()));
        assertThat(replace.containsKey("pattern"), is(true));
        assertThat(replace.get("pattern").toString(), is("JBAS"));        
        assertThat(replace.containsKey("replacement"), is(true));
        assertThat(replace.get("replacement").toString(), is("DUMMY"));
    }
}
