/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.host.controller;

import static org.jboss.as.host.controller.HostControllerLogger.ROOT_LOGGER;
import static org.jboss.as.host.controller.HostControllerMessages.MESSAGES;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.parsing.Attribute;
import org.jboss.as.controller.parsing.Element;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
class JvmOptionsBuilderFactory {

    private static final JvmOptionsBuilderFactory INSTANCE = new JvmOptionsBuilderFactory();

    private static final Map<JvmType, JvmOptionsBuilder> BUILDERS;
    static {
        Map<JvmType, JvmOptionsBuilder> map = new HashMap<JvmType, JvmOptionsBuilder>();
        map.put(JvmType.SUN, new SunJvmOptionsBuilder(JvmType.SUN));
        map.put(JvmType.IBM, new IbmJvmOptionsBuilder(JvmType.IBM));
        BUILDERS = Collections.unmodifiableMap(map);
    }

    private JvmOptionsBuilderFactory() {
    }

    static JvmOptionsBuilderFactory getInstance() {
        return INSTANCE;
    }

    void addOptions(JvmElement jvmElement, List<String> command){
        if (jvmElement == null) {
            throw MESSAGES.nullVar("jvm");
        }
        if (command == null) {
            throw MESSAGES.nullVar("command");
        }
        JvmOptionsBuilder builder = BUILDERS.get(jvmElement.getJvmType());
        if (builder == null) {
            throw MESSAGES.unknown("jvm", jvmElement.getJvmType());
        }
        builder.addToOptions(jvmElement, command);
    }

    private abstract static class JvmOptionsBuilder{
        final JvmType type;

        JvmOptionsBuilder(JvmType type) {
            this.type = type;
        }

        void addToOptions(JvmElement jvmElement, List<String> command){
            String heap = jvmElement.getHeapSize();
            String maxHeap = jvmElement.getMaxHeap();

            // FIXME not the correct place to establish defaults
            if (maxHeap == null && heap != null) {
                maxHeap = heap;
            }
            if (heap == null && maxHeap != null) {
                heap = maxHeap;
            }

            addPermGen(jvmElement, command);

            //Add to command
            if (heap != null) {
                command.add("-Xms"+ heap);
            }
            if (maxHeap != null) {
                command.add("-Xmx"+ maxHeap);
            }
            if (jvmElement.getStack() != null) {
                command.add("-Xss" + jvmElement.getStack());
            }
            if (jvmElement.getAgentPath() != null) {
                command.add("-agentpath:" + jvmElement.getAgentPath());
            }
            if (jvmElement.getAgentLib() != null) {
                command.add("-agentlib:" + jvmElement.getAgentLib());
            }
            if (jvmElement.getJavaagent() != null) {
                command.add("-javaagent:" + jvmElement.getJavaagent());
            }
            if (jvmElement.isDebugEnabled() != null && jvmElement.isDebugEnabled() && jvmElement.getDebugOptions() != null) {
                String debugOptions = jvmElement.getDebugOptions();
                if(debugOptions != null) {
                    if(! debugOptions.startsWith("-X")) {
                        debugOptions = "-X" + debugOptions;
                    }
                    command.add(debugOptions);
                }
            }
            List<String> options = jvmElement.getJvmOptions().getOptions();
            if (options.size() > 0) {
                String jvmName = jvmElement.getName();
                for (String option : options) {

                    if (!checkOption(heap != null && option.startsWith("-Xms"), jvmName, option, Element.HEAP.toString())) {
                        continue;
                    }
                    if (!checkOption(maxHeap != null && option.startsWith("-Xmx"), jvmName, option, Element.HEAP.toString())) {
                        continue;
                    }
                    if (!checkOption(jvmElement.getStack() != null && option.startsWith("-Xss"), jvmName, option, Element.STACK.toString())) {
                        continue;
                    }
                    if (!checkOption(jvmElement.getAgentPath() != null && option.startsWith("-agentpath:"), jvmName, option, Element.AGENT_PATH.toString())) {
                        continue;
                    }
                    if (!checkOption(jvmElement.getAgentLib() != null && option.startsWith("-agentlib:"), jvmName, option, Element.AGENT_LIB.toString())) {
                        continue;
                    }
                    if (!checkOption(jvmElement.getAgentLib() != null && option.startsWith("-javaagent:"), jvmName, option, Element.AGENT_LIB.toString())) {
                        continue;
                    }
                    if (!checkOption(jvmElement.getJavaagent() != null && option.startsWith("-Xmx"), jvmName, option, Element.JAVA_AGENT.toString())) {
                        continue;
                    }
                    if (!checkOption(jvmElement.getJavaagent() != null && option.startsWith("-XX:PermSize"), jvmName, option, Element.PERMGEN.toString())) {
                        continue;
                    }
                    if (!checkOption(jvmElement.getJavaagent() != null && option.startsWith("-XX:MaxPermSize"), jvmName, option, Element.PERMGEN.toString())) {
                        continue;
                    }
                    if (!checkOption(jvmElement.isDebugEnabled() != null && jvmElement.isDebugEnabled()
                            && jvmElement.getDebugOptions() != null && option.startsWith("-Xrunjdwp"), jvmName, option,
                            Attribute.DEBUG_OPTIONS.toString())) {
                        continue;
                    }
                    command.add(option);
                }
            }
        }

        boolean checkOption(boolean condition, String jvm, String option, String schemaElement) {
            if (condition) {
                ROOT_LOGGER.optionAlreadySet(option, jvm, schemaElement);
                return false;
            }
            return true;
        }

        abstract void addPermGen(JvmElement jvm, List<String> command);
    }

    private static class SunJvmOptionsBuilder extends JvmOptionsBuilder {

        public SunJvmOptionsBuilder(JvmType type) {
            super(type);
        }

        @Override
        void addPermGen(JvmElement jvmElement, List<String> command) {
            String permgen = jvmElement.getPermgenSize();
            String maxPermgen = jvmElement.getMaxPermgen();
            if (maxPermgen == null && permgen != null) {
                maxPermgen = permgen;
            }
            if (permgen == null && maxPermgen != null) {
                permgen = maxPermgen;
            }
            if (permgen != null) {
                command.add("-XX:PermSize=" + permgen);
            }
            if (maxPermgen != null) {
                command.add("-XX:MaxPermSize=" + maxPermgen);
            }
        }
    }

    private static class IbmJvmOptionsBuilder extends JvmOptionsBuilder {

        public IbmJvmOptionsBuilder(JvmType type) {
            super(type);
        }

        @Override
        void addPermGen(JvmElement jvmElement, List<String> command) {
            if (jvmElement.getPermgenSize() != null || jvmElement.getMaxPermgen() != null) {
                ROOT_LOGGER.ignoringPermGen(type, jvmElement.getName());
            }
        }
    }
}
