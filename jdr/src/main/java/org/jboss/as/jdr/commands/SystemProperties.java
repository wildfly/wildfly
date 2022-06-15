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
package org.jboss.as.jdr.commands;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Properties;

import org.jboss.as.jdr.util.Sanitizer;
import org.jboss.as.jdr.util.Utils;

/**
 * Add the JVM System properties to the JDR report
 *
 * @author Brad Maxwell
 */
public class SystemProperties extends JdrCommand {

    private LinkedList<Sanitizer> sanitizers = new LinkedList<Sanitizer>();

    public SystemProperties sanitizer(Sanitizer ... sanitizers) {
        for (Sanitizer s : sanitizers) {
            this.sanitizers.add(s);
        }
        return this;
    }

    @Override
    public void execute() throws Exception {
        if(!this.env.isServerRunning())
            return;

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        Properties properties = System.getProperties();

        Enumeration<?> names = properties.propertyNames();
        while(names.hasMoreElements()) {
            String name = (String) names.nextElement();
            printWriter.println(name + "=" + properties.getProperty(name));
        }
        InputStream stream = new ByteArrayInputStream(stringWriter.toString().getBytes(StandardCharsets.UTF_8));

        for (Sanitizer sanitizer : this.sanitizers) {
            stream = sanitizer.sanitize(stream);
        }

        this.env.getZip().addAsString(stream, "system-properties.txt");
        Utils.safelyClose(stream);
    }
}