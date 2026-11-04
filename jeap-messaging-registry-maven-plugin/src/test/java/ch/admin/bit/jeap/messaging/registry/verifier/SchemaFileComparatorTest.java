package ch.admin.bit.jeap.messaging.registry.verifier;

import ch.admin.bit.jeap.messaging.avro.plugin.compiler.ImportClassLoader;
import ch.admin.bit.jeap.messaging.registry.verifier.SchemaFileComparator.Comparison;
import ch.admin.bit.jeap.messaging.registry.verifier.SchemaFileComparator.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaFileComparatorTest {

    private static final String SCHEMA = """
            @namespace("ch.admin.test")
            protocol TestProtocol {
              import idl "ImportedA.avdl";
              import idl "ImportedB.avdl";

              // A comment with a link: https://example.com/old
              enum TestEnum {
                VALUE1,
                VALUE2,
                // default value for evolution compatibility
                UNKNOWN
              } = UNKNOWN;

              record TestNested {
                string doc;
              }

              /** The payload */
              record TestPayload {
                string id;
                union { null, string } name = null;
                TestEnum testEnum;
                ch.admin.test.ImportedA a;
                ImportedB b;
                TestNested nested = {"doc": "default"};
                timestamp_ms created;
              }
            }
            """;

    private static final String IMPORTED_A = """
            @namespace("ch.admin.test")
            protocol ImportedAProtocol {
              record ImportedA {
                string value;
              }
            }
            """;

    private static final String IMPORTED_B = """
            @namespace("ch.admin.test")
            protocol ImportedBProtocol {
              record ImportedB {
                string value;
              }
            }
            """;

    private static final String AVSC = """
            {"type": "record", "name": "TestRecord", "namespace": "ch.admin.test", "doc": "A record",
             "fields": [{"name": "id", "type": "string", "doc": "The id"}]}
            """;

    @TempDir
    private File tmpDir;
    private File oldDir;
    private File newDir;

    @BeforeEach
    void setUp() throws IOException {
        oldDir = new File(tmpDir, "old");
        newDir = new File(tmpDir, "new");
        for (File dir : new File[]{oldDir, newDir}) {
            Files.createDirectories(dir.toPath());
            write(dir, "ImportedA.avdl", IMPORTED_A);
            write(dir, "ImportedB.avdl", IMPORTED_B);
        }
    }

    static Stream<Arguments> avdlChanges() {
        return Stream.of(
                // Allowed: comments, documentation, formatting and declaration order
                change("changed line comment", s -> s.replace("https://example.com/old", "https://example.com/new"), Result.EQUIVALENT),
                change("added block comment", s -> s.replace("record TestNested", "/* nested */ record TestNested"), Result.EQUIVALENT),
                change("whitespace collapsed", s -> s.replaceAll("//[^\\n]*", "").replaceAll("\\s+", " "), Result.EQUIVALENT),
                change("CRLF line endings", s -> s.replace("\n", "\r\n"), Result.EQUIVALENT),
                change("tabs instead of spaces", s -> s.replace("  ", "\t"), Result.EQUIVALENT),
                change("changed doc comment", s -> s.replace("/** The payload */", "/** The payload, see https://example.com */"), Result.EQUIVALENT),
                change("added doc comment on field", s -> s.replace("string id;", "/** The id */ string id;"), Result.EQUIVALENT),
                change("reordered imports", s -> s.replace("""
                        import idl "ImportedA.avdl";
                          import idl "ImportedB.avdl";""", """
                        import idl "ImportedB.avdl";
                          import idl "ImportedA.avdl";"""), Result.EQUIVALENT),
                change("reordered type declarations", s -> s.replace("""
                          record TestNested {
                            string doc;
                          }
                        """, "").replace("// A comment with a link", """
                        record TestNested {
                            string doc;
                          }
                          // A comment with a link"""), Result.EQUIVALENT),
                // Rejected: any change of the schema
                change("renamed field", s -> s.replace("string id;", "string identifier;"), Result.CHANGED),
                change("swapped fields", s -> s.replace("string id;\n    union { null, string } name = null;",
                        "union { null, string } name = null;\n    string id;"), Result.CHANGED),
                change("removed field default", s -> s.replace("name = null;", "name;"), Result.CHANGED),
                change("changed key named doc in default value", s -> s.replace("{\"doc\": \"default\"}", "{\"doc\": \"other\"}"), Result.CHANGED),
                change("added field alias", s -> s.replace("string id;", "string @aliases([\"identifier\"]) id;"), Result.CHANGED),
                change("added type alias", s -> s.replace("record TestPayload", "@aliases([\"ch.admin.test.OldPayload\"]) record TestPayload"), Result.CHANGED),
                change("changed enum default", s -> s.replace("} = UNKNOWN;", "} = VALUE1;"), Result.CHANGED),
                change("reordered enum symbols", s -> s.replace("VALUE1,\n    VALUE2,", "VALUE2,\n    VALUE1,"), Result.CHANGED),
                change("added custom property", s -> s.replace("record TestPayload", "@foo(\"bar\") record TestPayload"), Result.CHANGED),
                change("changed logical type", s -> s.replace("timestamp_ms created;", "long created;"), Result.CHANGED),
                change("added type", s -> s.replace("record TestNested", "record Other { string value; }\n  record TestNested"), Result.CHANGED),
                change("not parsable anymore", s -> s.replace("string id;", "string id"), Result.CHANGED)
        );
    }

    private static Arguments change(String name, UnaryOperator<String> change, Result expectedResult) {
        return Arguments.of(name, change, expectedResult);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("avdlChanges")
    void compareAvdl(String name, UnaryOperator<String> change, Result expectedResult) throws IOException {
        String changed = change.apply(SCHEMA);
        assertThat(changed).as("test setup: change must apply").isNotEqualTo(SCHEMA);
        File oldFile = write(oldDir, "Test.avdl", SCHEMA);
        File newFile = write(newDir, "Test.avdl", changed);

        Comparison comparison = compare(oldFile, newFile);

        assertThat(comparison.result()).as(comparison.detail()).isEqualTo(expectedResult);
    }

    @Test
    void compareAvdl_identicalFiles_areNotParsed() throws IOException {
        File oldFile = write(oldDir, "Test.avdl", SCHEMA);
        File newFile = write(newDir, "Test.avdl", SCHEMA);
        Supplier<ImportClassLoader> notToBeUsed = () -> {
            throw new AssertionError("identical files must not be parsed");
        };

        Comparison comparison = SchemaFileComparator.compare(oldFile, notToBeUsed, newFile, notToBeUsed);

        assertThat(comparison.result()).isEqualTo(Result.IDENTICAL);
    }

    @Test
    void compareAvdl_changedType_isNamedInDetail() throws IOException {
        File oldFile = write(oldDir, "Test.avdl", SCHEMA);
        File newFile = write(newDir, "Test.avdl", SCHEMA.replace("string id;", "string identifier;"));

        Comparison comparison = compare(oldFile, newFile);

        assertThat(comparison.detail()).isEqualTo("ch.admin.test.TestPayload changed");
    }

    @Test
    void compareAvdl_importedTypeChanged_resolvesImportsAgainstOwnDirectory() throws IOException {
        // the files must differ, otherwise they are not parsed at all
        File oldFile = write(oldDir, "Test.avdl", SCHEMA + "\n");
        File newFile = write(newDir, "Test.avdl", SCHEMA);
        write(newDir, "ImportedA.avdl", IMPORTED_A.replace("string value;", "string value;\n    int count;"));

        Comparison comparison = compare(oldFile, newFile);

        assertThat(comparison.result()).isEqualTo(Result.CHANGED);
        assertThat(comparison.detail()).contains("ch.admin.test.ImportedA changed");
    }

    @Test
    void compareAvdl_notParsable_detailContainsParseError() throws IOException {
        File oldFile = write(oldDir, "Test.avdl", SCHEMA);
        File newFile = write(newDir, "Test.avdl", SCHEMA.replace("string id;", "string id"));

        Comparison comparison = compare(oldFile, newFile);

        assertThat(comparison.result()).isEqualTo(Result.CHANGED);
        assertThat(comparison.detail()).startsWith("cannot parse " + newFile.getAbsolutePath());
    }

    @Test
    void compareAvsc_formattingAndDocumentationChanged_isEquivalent() throws IOException {
        File oldFile = write(oldDir, "Test.avsc", AVSC);
        File newFile = write(newDir, "Test.avsc", AVSC.replace(", ", ",\n  ").replace("The id", "The identifier"));

        assertThat(compare(oldFile, newFile).result()).isEqualTo(Result.EQUIVALENT);
    }

    @Test
    void compareAvsc_fieldTypeChanged_isChanged() throws IOException {
        File oldFile = write(oldDir, "Test.avsc", AVSC);
        File newFile = write(newDir, "Test.avsc", AVSC.replace("\"type\": \"string\"", "\"type\": \"long\""));

        assertThat(compare(oldFile, newFile).result()).isEqualTo(Result.CHANGED);
    }

    @Test
    void compareOtherFile_contentChanged_isChanged() throws IOException {
        File oldFile = write(oldDir, "Test.txt", "old");
        File newFile = write(newDir, "Test.txt", "new");

        assertThat(compare(oldFile, newFile).result()).isEqualTo(Result.CHANGED);
    }

    private static Comparison compare(File oldFile, File newFile) throws IOException {
        return SchemaFileComparator.compare(oldFile, ImportClassLoader::new, newFile, ImportClassLoader::new);
    }

    private static File write(File dir, String filename, String content) throws IOException {
        File file = new File(dir, filename);
        Files.writeString(file.toPath(), content);
        return file;
    }
}
