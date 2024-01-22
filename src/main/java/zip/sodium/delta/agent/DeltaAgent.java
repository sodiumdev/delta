package zip.sodium.delta.agent;

import org.bukkit.Material;
import org.objectweb.asm.*;
import org.objectweb.asm.util.TraceClassVisitor;
import oshi.util.tuples.Pair;
import zip.sodium.delta.api.Delta;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class DeltaAgent {
    public static final List<Pair<String, Boolean>> FIELD_STACK = new ArrayList<>(256);
    public static final int DIVIDER = 128;

    public static class DeltaClassTransformer implements ClassFileTransformer {
        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
            final var materialClassName = "org/bukkit/Material";
            final var materialDescriptor = "L" + materialClassName + ";";

            if (!className.equals(materialClassName))
                return classfileBuffer;

            final var reader = new ClassReader(classfileBuffer);

            final var writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

            final FileWriter fileWriter;
            try {
                fileWriter = new FileWriter("code.txt");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            final var visitor = new ClassVisitor(
                    Opcodes.ASM9, new TraceClassVisitor(writer, new PrintWriter(fileWriter))) {
                private int originalLength;

                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    final var method = super.visitMethod(access, name, descriptor, signature, exceptions);

                    if (name.equals("$values")) {
                        return new MethodVisitor(api, method) {
                            private int sipushIndex = 0;

                            private int modifiedLength;

                            @Override
                            public void visitIntInsn(int opcode, int operand) {
                                if (opcode == Opcodes.SIPUSH) {
                                    if (sipushIndex == 0) {
                                        originalLength = operand;
                                        modifiedLength = operand;

                                        operand += FIELD_STACK.size();
                                    }

                                    sipushIndex++;
                                }

                                super.visitIntInsn(opcode, operand);
                            }

                            @Override
                            public void visitInsn(int opcode) {
                                if (opcode != Opcodes.ARETURN) {
                                    super.visitInsn(opcode);

                                    return;
                                }

                                FIELD_STACK.forEach((pair) -> this.visitMaterialStore(pair.getA()));

                                super.visitInsn(Opcodes.ARETURN);
                            }

                            private void visitMaterialStore(final String name) {
                                visitInsn(Opcodes.DUP);

                                visitIntInsn(Opcodes.SIPUSH, modifiedLength);
                                visitFieldInsn(
                                        Opcodes.GETSTATIC,
                                        materialClassName,
                                        name,
                                        materialDescriptor
                                );
                                visitInsn(Opcodes.AASTORE);

                                modifiedLength++;
                            }
                        };
                    }

                    if (name.equals("<clinit>")) {
                        return new MethodVisitor(api, method) {
                            private int modifiedLength = -1;

                            @Override
                            public void visitInsn(int opcode) {
                                if (opcode == Opcodes.RETURN)
                                    FIELD_STACK.stream().filter(Pair::getB).forEach(x -> {
                                        super.visitFieldInsn(
                                                Opcodes.GETSTATIC,
                                                materialClassName,
                                                x.getA(),
                                                materialDescriptor
                                        );

                                        super.visitInsn(Opcodes.ICONST_1);
                                        super.visitFieldInsn(
                                                Opcodes.PUTFIELD,
                                                materialClassName,
                                                "isBlock",
                                                "Z"
                                        );
                                    });

                                super.visitInsn(opcode);
                            }

                            @Override
                            public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                                if (modifiedLength == -1)
                                    modifiedLength = originalLength;

                                if (opcode != Opcodes.PUTSTATIC
                                        || !name.equals("LEGACY_RECORD_12")) {
                                    super.visitFieldInsn(opcode, owner, name, descriptor);

                                    return;
                                }

                                super.visitFieldInsn(opcode, owner, name, descriptor);

                                FIELD_STACK.forEach((pair) -> this.visitMaterialField(pair.getA()));
                            }

                            private void visitMaterialField(final String name) {
                                visitTypeInsn(
                                        Opcodes.NEW,
                                        materialClassName
                                );
                                visitInsn(Opcodes.DUP);

                                visitLdcInsn(name);
                                visitLdcInsn(modifiedLength);
                                visitLdcInsn(-1);

                                visitMethodInsn(
                                        Opcodes.INVOKESPECIAL,
                                        materialClassName,
                                        "<init>",
                                        "(Ljava/lang/String;II)V",
                                        false
                                );

                                super.visitFieldInsn(
                                        Opcodes.PUTSTATIC,
                                        materialClassName,
                                        name,
                                        materialDescriptor
                                );

                                modifiedLength++;
                            }
                        };
                    }

                    return method;
                }
            };

            FIELD_STACK.forEach(newFieldName -> writer.visitField(
                    Opcodes.ACC_PUBLIC
                            | Opcodes.ACC_STATIC
                            | Opcodes.ACC_FINAL
                            | Opcodes.ACC_ENUM,
                    newFieldName.getA(),
                    materialDescriptor,
                    null,
                    null
            ));

            reader.accept(visitor, 0);

            FIELD_STACK.clear();

            System.out.println("do we get here");

            return writer.toByteArray();
        }
    }

    private static Instrumentation instrumentation;

    public static void agentmain(String agentArgs, Instrumentation inst) {
        instrumentation = inst;

        for (int i = 0; i < DIVIDER * 2; i++) {
            FIELD_STACK.add(
                    new Pair<>(
                            "DELTA_MAT_" + i,
                            i > (DIVIDER - 1)
                    )
            );
        }

        inst.addTransformer(new DeltaClassTransformer());

        transformMaterial();

        Delta.init();
    }

    public static void transformMaterial() {
        try {
            instrumentation.retransformClasses(Material.class);
        } catch (UnmodifiableClassException e) {
            throw new RuntimeException(e);
        }
    }

    public static Instrumentation getInstrumentation() {
        return instrumentation;
    }
}
