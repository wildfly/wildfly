/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.demos.jpa.mbean;

import org.jboss.as.demos.jpa.archive.SimpleStatefulSessionLocal;

import javax.naming.InitialContext;
import java.util.concurrent.Callable;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class ExerciseStateful implements Callable<String> {
    @Override
    public String call() throws Exception {
        InitialContext ctx = new InitialContext();
        String name = "java:global/jpa-example/SimpleStatefulSessionBean!" + SimpleStatefulSessionLocal.class.getName();
        SimpleStatefulSessionLocal bean = (SimpleStatefulSessionLocal) ctx.lookup(name);
        bean.setState("42");

        return "Invoke echo under transaction "+
            bean.echo("answer is")
            //+
            //".  Invoke echoNoTx without a transaction " +
            //bean.echoNoTx("demo")
            ;
        // TODO:  uncomment call to echoNoTx when exception doesn't cause system failure (at least that is how it looks now).
    }
}
