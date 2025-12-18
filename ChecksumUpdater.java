/// usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 17+
//DEPS info.picocli:picocli:4.7.7

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;

import picocli.AutoComplete;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * This is used to hack around the missing md5 and sha1 checksum files required by Maven Central. Specifically for the
 * POM's build by the wildfly-bom-builder-maven-plugin.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@Command(name = "checksum-updater", mixinStandardHelpOptions = true, version = "checksum-updater 0.1",
        description = "Creates missing checksums for POM files.",
        showDefaultValues = true, subcommands = AutoComplete.GenerateCompletion.class)
public class ChecksumUpdater implements Callable<Integer> {

    private static final char[] table = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    @Option(names = {"-a", "--algorithm"}, description = "The algorithm(s) for the generated checksum", split = ",", defaultValue = "md5,sha1")
    private String[] algorithms;

    @Parameters(arity = "1..*", description = "The directories which contain the file to create the checksums for if they are missing")
    private Path[] dirs;

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    public static void main(String... args) {
        final int exitCode = new CommandLine(new ChecksumUpdater()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        for (Path dir : dirs) {
            if (!Files.isDirectory(dir)) {
                spec.commandLine().getErr().printf("%s must be a directory. Skipping processing.%n", dir);
                continue;
            }
            // Walk the ZIP tree looking for missing checksum files and add them
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    if (file.toString().endsWith(".pom")) {
                        for (String algorithm : algorithms) {
                            final Path checksumFile = Path.of(file.toAbsolutePath() + "." + algorithm.toLowerCase(Locale.ROOT));
                            if (Files.notExists(checksumFile)) {
                                try {
                                    final var md = MessageDigest.getInstance(algorithm.toUpperCase(Locale.ROOT));
                                    try (DigestInputStream dis = new DigestInputStream(Files.newInputStream(file), md)) {
                                        final byte[] buffer = new byte[1024];
                                        while (dis.read(buffer) > 0) {
                                            // Just gathering
                                        }
                                        final byte[] checksum = md.digest();
                                        final String checksumString = bytesToHexString(checksum);
                                        if (Files.exists(checksumFile)) {
                                            Files.delete(checksumFile);
                                        }
                                        Files.writeString(checksumFile, checksumString, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
                                    }
                                } catch (NoSuchAlgorithmException e) {
                                    spec.commandLine()
                                            .getErr()
                                            .printf("Algorithm %s not found: %s%n", algorithm, e.getMessage());
                                }
                            }
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        return 0;
    }

    private static String bytesToHexString(final byte[] bytes) {
        final StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(table[b >> 4 & 0x0f]).append(table[b & 0x0f]);
        }
        return builder.toString();
    }
}
