/*
 * Copyright 2012 David Blevins
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.jboss.as.test.integration.ejb.mdb.dynamic.application;

import org.jboss.as.test.integration.ejb.mdb.dynamic.api.Command;
import org.jboss.as.test.integration.ejb.mdb.dynamic.api.Option;
import org.jboss.as.test.integration.ejb.mdb.dynamic.api.TelnetListener;
import org.jboss.ejb3.annotation.ResourceAdapter;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "beanClass", propertyValue = "org.jboss.as.test.integration.ejb.mdb.dynamic.application.MyMdb"),
        @ActivationConfigProperty(propertyName = "prompt", propertyValue = "pronto>")
})
@ResourceAdapter("ear-with-rar.ear#telnet-ra.rar")
public class MyMdb implements TelnetListener {
    private final Properties properties = new Properties();

    @Command("get")
    public String doGet(@Option("key") String key) {
        return properties.getProperty(key);
    }

    @Command("set")
    public String doSet(@Option("key") String key, @Option("value") String value) {

        final Object old = properties.setProperty(key, value);
        final StringBuilder sb = new StringBuilder();
        sb.append("set ").append(key).append(" to ").append(value);
        sb.append("\n");
        if (old != null) {
            sb.append("old value: ").append(old);
            sb.append("\n");
        }
        return sb.toString();
    }

    @Command("list")
    public String doList(@Option("pattern") Pattern pattern) {

        if (pattern == null) pattern = Pattern.compile(".*");
        final StringBuilder sb = new StringBuilder();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            final String key = entry.getKey().toString();
            if (pattern.matcher(key).matches()) {
                sb.append(key).append(" = ").append(entry.getValue()).append("\n");
            }
        }
        return sb.toString();
    }
}