package rs.nopressurewear.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/** Source-level boundary checks run in the normal Maven test phase, without a database. */
class ModuleBoundariesTest {
    private static final String ROOT = "rs.nopressurewear.";
    private static final Path SOURCES = Path.of("src/main/java");
    private static final Set<String> MODULES = Set.of("identity", "catalog", "shopping", "ordering",
            "promotions", "payments", "storefront", "notifications", "media", "reporting", "platform");
    private static final Pattern PACKAGE = Pattern.compile("(?m)^package ([\\w.]+);");
    private static final Pattern REFERENCE = Pattern.compile("rs\\.nopressurewear(?:\\.[A-Za-z_$][\\w$]*)+");
    private static final Pattern WILDCARD = Pattern.compile("(?m)^import (?:static )?rs\\.nopressurewear\\.[\\w.]+\\.\\*;");

    @Test
    void sourcesBelongToNamedModulesAndMatchTheirDirectories() throws IOException {
        for (var source : sources().entrySet()) {
            String type = source.getKey();
            if (type.equals(ROOT + "NopressureWearWebshopApplication")) continue;
            String module = module(type);
            assertThat(MODULES).as(type).contains(module);
            String remainder = type.substring((ROOT + module + ".").length());
            assertThat(remainder).as(type).matches("(?:api|internal|error|util|config)\\..+");
            assertThat(WILDCARD.matcher(source.getValue()).find()).as("explicit project imports in " + type).isFalse();
        }
    }

    @Test
    void publicContractsAndPlatformDoNotDependOnBusinessInternals() throws IOException {
        Map<String, String> sources = sources();
        for (var source : sources.entrySet()) {
            String type = source.getKey();
            boolean platform = module(type).equals("platform");
            boolean api = type.contains(".api.");
            if (!platform && !api) continue;
            for (String target : references(source.getValue(), sources.keySet())) {
                if (platform) {
                    assertThat(module(target)).as(type + " -> " + target).isEqualTo("platform");
                } else {
                    assertThat(target).as(type + " -> " + target).doesNotContain(".internal.");
                }
            }
        }
    }

    @Test
    void noNewAccessToAnotherModulesInternals() throws IOException {
        Map<String, String> sources = sources();
        Set<String> actual = new TreeSet<>();
        for (var source : sources.entrySet()) {
            for (String target : references(source.getValue(), sources.keySet())) {
                if (!module(source.getKey()).equals(module(target)) && target.contains(".internal.")) {
                    actual.add(source.getKey() + " -> " + target);
                }
            }
        }
        Set<String> legacy = new TreeSet<>();
        for (String line : Files.readAllLines(Path.of("src/test/resources/architecture/legacy-module-access.txt"))) {
            if (!line.isBlank() && !line.startsWith("#")) legacy.add(line);
        }
        Set<String> newAccess = new TreeSet<>(actual);
        newAccess.removeAll(legacy);
        assertThat(newAccess).as("Use the owning module's api; do not extend the migration baseline").isEmpty();
        Set<String> staleExceptions = new TreeSet<>(legacy);
        staleExceptions.removeAll(actual);
        assertThat(staleExceptions).as("Remove resolved entries from legacy-module-access.txt").isEmpty();
    }

    private static Map<String, String> sources() throws IOException {
        Map<String, String> result = new HashMap<>();
        try (var files = Files.walk(SOURCES)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                if (file.getFileName().toString().equals("package-info.java")) continue;
                String text = Files.readString(file);
                var matcher = PACKAGE.matcher(text);
                assertThat(matcher.find()).as("package in " + file).isTrue();
                String type = matcher.group(1) + "." + file.getFileName().toString().replace(".java", "");
                assertThat(SOURCES.resolve(type.replace('.', '/') + ".java").normalize()).isEqualTo(file.normalize());
                result.put(type, text);
            }
        }
        return result;
    }

    private static Set<String> references(String text, Set<String> types) {
        Set<String> result = new HashSet<>();
        var matcher = REFERENCE.matcher(text);
        while (matcher.find()) {
            String candidate = matcher.group();
            while (candidate.startsWith(ROOT)) {
                if (types.contains(candidate)) {
                    result.add(candidate);
                    break;
                }
                candidate = candidate.substring(0, candidate.lastIndexOf('.'));
            }
        }
        return result;
    }

    private static String module(String type) {
        return type.substring(ROOT.length()).split("\\.")[0];
    }
}
