package ch.admin.bit.jeap.messaging.annotations.processor.util;

import java.lang.classfile.ClassFile;
import java.lang.classfile.Instruction;
import java.lang.classfile.instruction.ConstantInstruction;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.classfile.instruction.NewObjectInstruction;
import java.lang.classfile.instruction.NewReferenceArrayInstruction;
import java.util.List;
import java.util.Optional;
import java.lang.reflect.AccessFlag;

import static java.lang.classfile.Opcode.*;

/**
 * Recognizes only Avro's generated, straight-line SCHEMA$ initializer at the start of clinit.
 * This is deliberately not a bytecode interpreter: unknown expressions fall back to the registry.
 * No message classes, parsers, conversions or logging providers are loaded or executed.
 */
final class SchemaClassFile {
    private static final String PARSER = "org/apache/avro/Schema$Parser";
    private static final String SCHEMA = "Lorg/apache/avro/Schema;";

    private SchemaClassFile() { }

    static Optional<String> read(byte[] bytes, String binaryName) {
        var model = ClassFile.of().parse(bytes);
        if (!model.thisClass().asInternalName().equals(binaryName.replace('.', '/'))) {
            return Optional.empty();
        }
        if (model.fields().stream().noneMatch(field -> field.fieldName().equalsString("SCHEMA$")
                && field.fieldType().equalsString(SCHEMA) && field.flags().has(AccessFlag.STATIC)
                && field.flags().has(AccessFlag.FINAL))) {
            return Optional.empty();
        }
        var initializer = model.methods().stream().filter(method -> method.methodName().equalsString("<clinit>"))
                .findFirst().flatMap(method -> method.code());
        if (initializer.isEmpty() || !initializer.get().exceptionHandlers().isEmpty()) {
            return Optional.empty();
        }
        List<Instruction> instructions = initializer.get().elementStream()
                .filter(Instruction.class::isInstance).map(Instruction.class::cast).toList();
        // Multiple assignments are not the generated form and cannot safely be reduced to one literal.
        if (instructions.stream().filter(instruction -> schemaAssignment(instruction, binaryName)).count() != 1) {
            return Optional.empty();
        }
        try {
            var cursor = new Cursor(instructions);
            if (!(cursor.next() instanceof NewObjectInstruction allocation)
                    || !allocation.className().asInternalName().equals(PARSER)
                    || cursor.next().opcode() != DUP
                    || !invoke(cursor.next(), INVOKESPECIAL, "<init>", "()V")) {
                return Optional.empty();
            }
            StringBuilder json = new StringBuilder(cursor.string());
            if (cursor.peek() instanceof ConstantInstruction size && size.constantValue() instanceof Integer length) {
                cursor.next();
                if (length < 0 || length > instructions.size()
                        || !(cursor.next() instanceof NewReferenceArrayInstruction array)
                        || !array.componentType().asInternalName().equals("java/lang/String")) {
                    return Optional.empty();
                }
                for (int index = 0; index < length; index++) {
                    if (cursor.next().opcode() != DUP || !(cursor.next() instanceof ConstantInstruction position)
                            || !Integer.valueOf(index).equals(position.constantValue())) {
                        return Optional.empty();
                    }
                    json.append(cursor.string());
                    if (cursor.next().opcode() != AASTORE) return Optional.empty();
                }
                if (!invoke(cursor.next(), INVOKEVIRTUAL, "parse", "(Ljava/lang/String;[Ljava/lang/String;)" + SCHEMA)) {
                    return Optional.empty();
                }
            } else if (!invoke(cursor.next(), INVOKEVIRTUAL, "parse", "(Ljava/lang/String;)" + SCHEMA)) {
                return Optional.empty();
            }
            return schemaAssignment(cursor.next(), binaryName) ? Optional.of(json.toString()) : Optional.empty();
        } catch (IndexOutOfBoundsException | IllegalArgumentException unsupported) {
            return Optional.empty();
        }
    }

    private static boolean invoke(Instruction instruction, java.lang.classfile.Opcode opcode, String name, String type) {
        return instruction instanceof InvokeInstruction call && call.opcode() == opcode
                && call.owner().asInternalName().equals(PARSER) && call.name().equalsString(name) && call.type().equalsString(type);
    }

    private static boolean schemaAssignment(Instruction instruction, String binaryName) {
        return instruction instanceof FieldInstruction field && field.opcode() == PUTSTATIC
                && field.owner().asInternalName().equals(binaryName.replace('.', '/'))
                && field.name().equalsString("SCHEMA$") && field.type().equalsString(SCHEMA);
    }

    private static final class Cursor {
        private final List<Instruction> instructions;
        private int index;

        private Cursor(List<Instruction> instructions) { this.instructions = instructions; }
        private Instruction peek() { return instructions.get(index); }
        private Instruction next() { return instructions.get(index++); }
        private String string() {
            if (next() instanceof ConstantInstruction constant && constant.constantValue() instanceof String text) return text;
            throw new IllegalArgumentException("Expected a string literal");
        }
    }
}
