/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat Middleware LLC, and individual contributors
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

package org.wildfly.build.plugin;

import org.apache.maven.plugin.logging.Log;
import org.jboss.modules.ModuleIdentifier;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Stuart Douglas
 */
public class ModuleUtils {


    private ModuleUtils() {

    }

    public static Map<ModuleIdentifier, ModuleParseResult> enumerateModuleDirectory(Log log, final Path moduleDirectory) throws IOException {
        ModuleVisitor visitor = new ModuleVisitor(moduleDirectory, log);
        Files.walkFileTree(moduleDirectory, visitor);
        return visitor.getParseResults();
    }

    private static final class ModuleVisitor implements FileVisitor<Path> {

        private final Path moduleRoot;
        final Log log;
        private final Map<ModuleIdentifier, ModuleParseResult> parseResults = new HashMap<>();

        private ModuleVisitor(Path moduleRoot, Log log) {
            this.moduleRoot = moduleRoot;
            this.log = log;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if(!file.getName(file.getNameCount() - 1).toString().equals("module.xml")) {
                return FileVisitResult.CONTINUE;
            }
            try {
                ModuleParseResult result = ModuleParser.parse(moduleRoot, file);
                parseResults.put(result.getIdentifier(), result);
            } catch (XMLStreamException e) {
                throw new RuntimeException(e);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            return FileVisitResult.TERMINATE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        private Map<ModuleIdentifier, ModuleParseResult> getParseResults() {
            return parseResults;
        }
    }
}
