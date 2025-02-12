/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.installer.plugin.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * A @{link {@link FileVisitor} copies directory trees keeping a note of all copied target files.
 */
public final class LoggingVisitor extends SimpleFileVisitor<Path> {
    
    /** logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(LoggingVisitor.class);

    /** How what files have we copied? */
    @Nonnull private final List<Path> copiedFiles = new ArrayList<>();

    /** Path we are traversing. */
    @Nonnull private final Path from;
    
    /** Path where we copy to. */
    @Nonnull private final Path to;
    
    /**
     * Constructor.
     *
     * @param fromDir Path we are traversing
     * @param toDir Path where we check for Duplicates
     */
    public LoggingVisitor(@Nonnull final Path fromDir, @Nonnull final Path toDir) {
        from = fromDir;
        to = toDir;
    }

    @Override
    public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
        final Path relDir = from.relativize(dir);
        final Path toDir = to.resolve(relDir);
        if (!Files.exists(toDir)) {
            log.trace("Creating directory {}", toDir);
            Files.createDirectory(toDir);
        }
        return FileVisitResult.CONTINUE;
    };

    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
        final Path relFile = from.relativize(file);
        final Path toFile = to.resolve(relFile);
        copiedFiles.add(toFile);
        try(final InputStream in = new BufferedInputStream(new FileInputStream(file.toFile()));
            final OutputStream out = new BufferedOutputStream(new FileOutputStream(toFile.toFile()))) {
            in.transferTo(out);
        }
        return FileVisitResult.CONTINUE;
    }
    
    /**
     * Did we find a name clash?
     * 
     * @return whether we found a name clash
     */
    @Nonnull public List<Path> getCopiedList() {
        return copiedFiles;
    }

}