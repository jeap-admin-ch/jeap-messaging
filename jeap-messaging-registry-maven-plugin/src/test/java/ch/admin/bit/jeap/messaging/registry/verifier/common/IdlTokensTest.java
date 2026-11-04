package ch.admin.bit.jeap.messaging.registry.verifier.common;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdlTokensTest {

    private static final String SCHEMA = """
            @namespace("ch.admin.test")
            protocol TestProtocol {
              // import idl "CommentedOut.avdl";
              import idl "First.avdl";
              import
                idl   "Second.avdl";
              import schema "Schema.avsc";

              /* record InComment { NotUsed a; } */
              enum TestEnum { A, B } = A;

              @namespace("ch.admin.other")
              fixed TestFixed(16);

              error TestError {
                ErrorDetail detail;
              }

              /** A record, see https://example.com */
              @aliases(["ch.admin.test.OldRecord"])
              record TestRecord {
                string id;
                union { null, ch.admin.imported.QualifiedType } qualified = null;
                SimpleType
                  simple;
                array<ItemType> items;
                map<ValueType> values;
                @java-class("java.math.BigDecimal") string amount;
                OtherType other = {"key": "LiteralNotAName"};
              }

              record `error` {
                string value;
              }

              record ch.admin.qualified.QualifiedRecord {
                string value;
              }
            }
            """;

    @Test
    void idlImports_onlyIdlImportsOutsideOfComments() throws IOException {
        assertThat(read(SCHEMA).idlImports()).containsExactly("First.avdl", "Second.avdl");
    }

    @Test
    void namesUsedInRecords_namesOfFieldTypesAndFieldsWithSimpleNamesOfQualifiedNames() throws IOException {
        Set<String> names = read(SCHEMA).namesUsedInRecords();

        assertThat(names).contains(
                "ErrorDetail", "SimpleType", "ItemType", "ValueType", "OtherType",
                "ch.admin.imported.QualifiedType", "QualifiedType");
        assertThat(names)
                .as("names in comments, annotations and string literals are not used")
                .doesNotContain("NotUsed", "aliases", "java", "LiteralNotAName", "ch.admin.test.OldRecord");
        assertThat(names)
                .as("names used outside of records and errors")
                .doesNotContain("TestProtocol", "TestEnum", "TestFixed", "First", "namespace");
    }

    @Test
    void namesUsedInRecords_qualifiedNameIsOneName() throws IOException {
        Set<String> names = read("""
                protocol P {
                  record R {
                    a.b.C c;
                  }
                }
                """).namesUsedInRecords();

        assertThat(names).containsExactlyInAnyOrder("a.b.C", "C", "c");
    }

    @Test
    void declaredTypes_namespacesFromProtocolAnnotationAndQualifiedName() throws IOException {
        Set<AvroTypeInfo> types = read(SCHEMA).declaredTypes();

        assertThat(types).hasSize(6);
        assertDeclared(types, "ch.admin.test.TestEnum");
        assertDeclared(types, "ch.admin.other.TestFixed");
        assertDeclared(types, "ch.admin.test.TestError");
        assertDeclared(types, "ch.admin.test.TestRecord");
        assertDeclared(types, "ch.admin.test.error");
        assertDeclared(types, "ch.admin.qualified.QualifiedRecord");
        assertThat(types).noneMatch(type -> type.isSameType("InComment"));
    }

    @Test
    void declaredTypes_importProtocolKeepsProtocolNamespace() throws IOException {
        Set<AvroTypeInfo> types = read("""
                @namespace("ch.admin.test")
                protocol P {
                  import protocol "Other.avpr";
                  record R { string value; }
                }
                """).declaredTypes();

        assertThat(types).singleElement().matches(type -> type.isSameType("ch.admin.test.R"));
    }

    @Test
    void declaredTypes_withoutProtocolNamespace() throws IOException {
        Set<AvroTypeInfo> types = read("protocol P { record R { string value; } }").declaredTypes();

        assertThat(types).singleElement().matches(type -> type.isSameType("R"));
    }

    @Test
    void read_notUtf8InComment_isRead() throws IOException {
        byte[] latin1 = "protocol P { // Zollübergang\n import idl \"A.avdl\"; }".getBytes(StandardCharsets.ISO_8859_1);

        assertThat(IdlTokens.read(new ByteArrayInputStream(latin1)).idlImports()).containsExactly("A.avdl");
    }

    @Test
    void read_unterminatedComment_throwsIOException() {
        assertThatThrownBy(() -> read("protocol P { /* unterminated comment"))
                .isInstanceOf(IOException.class)
                .hasMessageStartingWith("Invalid Avro IDL");
    }

    @Test
    void read_invalidUnicodeEscapeInComment_throwsIOException() {
        assertThatThrownBy(() -> read("protocol P { // see C:\\users\\test\n }"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Invalid escape character");
    }

    private static void assertDeclared(Set<AvroTypeInfo> types, String fullName) {
        assertThat(types).as("declares %s", fullName).anyMatch(type -> type.isSameType(fullName));
    }

    private static IdlTokens read(String idl) throws IOException {
        return IdlTokens.read(new ByteArrayInputStream(idl.getBytes(StandardCharsets.UTF_8)));
    }
}
