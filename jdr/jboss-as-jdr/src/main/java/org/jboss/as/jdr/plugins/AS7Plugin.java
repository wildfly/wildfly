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

package org.jboss.as.jdr.plugins;

import java.util.Arrays;
import java.util.List;

import org.jboss.as.jdr.commands.CallAS7;
import org.jboss.as.jdr.commands.CollectFiles;
import org.jboss.as.jdr.commands.DeploymentDependencies;
import org.jboss.as.jdr.commands.JarCheck;
import org.jboss.as.jdr.commands.JdrCommand;
import org.jboss.as.jdr.commands.LocalModuleDependencies;
import org.jboss.as.jdr.commands.SystemProperties;
import org.jboss.as.jdr.commands.TreeCommand;
import org.jboss.as.jdr.util.Sanitizer;
import org.jboss.as.jdr.util.Sanitizers;
import org.jboss.as.jdr.util.Utils;

public class AS7Plugin implements JdrPlugin {

    private final PluginId pluginId = new PluginId("AS7_PLUGIN", 1, 0, null);

    @Override
    public List<JdrCommand> getCommands() throws Exception {
        Sanitizer xmlSanitizer = Sanitizers.xml("//password");
        Sanitizer passwordSanitizer = Sanitizers.pattern("password=.*", "password=*");
        Sanitizer systemPropertiesPasswordSanitizer = Sanitizers.pattern("([^=]*password[^=]*)=.*", "$1=*");

        return Arrays.asList(
            new TreeCommand(),
            new JarCheck(),
            new CallAS7("configuration").param("recursive", "true"),
            new CallAS7("dump-services").operation("dump-services").resource("core-service", "service-container"),
            new CallAS7("cluster-proxies-configuration").resource("subsystem", "modcluster"),
            new CallAS7("jndi-view").operation("jndi-view").resource("subsystem", "naming"),
            new CollectFiles("*/standalone/configuration/*").sanitizer(xmlSanitizer, passwordSanitizer),
            new CollectFiles("*/domain/configuration/*").sanitizer(xmlSanitizer, passwordSanitizer),
            new CollectFiles("*server.log").limit(50 * Utils.ONE_MB),
            new CollectFiles("*.log").omit("*server.log"),
            new CollectFiles("*gc.log.*"),
            new CollectFiles("*.properties").sanitizer(passwordSanitizer),
            new CollectFiles("*.xml").sanitizer(xmlSanitizer),
            new CollectFiles("*/modules/system/*/.overlays/.overlays"),
            new CollectFiles("*/.installation/*.conf"),
            new CollectFiles("*/.installation/*.txt"),
            new SystemProperties().sanitizer(systemPropertiesPasswordSanitizer),
            new DeploymentDependencies(),
            new LocalModuleDependencies()
        );
    }

    @Override
    public PluginId getPluginId() {
        return pluginId;
    }
}
