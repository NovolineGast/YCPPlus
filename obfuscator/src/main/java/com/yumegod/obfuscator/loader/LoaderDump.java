package com.yumegod.obfuscator.loader;

import org.objectweb.asm.*;

public class LoaderDump implements Opcodes {

    // generate by asm plugin
    public static ClassWriter dump() throws Exception {

        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        MethodVisitor methodVisitor;

        classWriter.visit(V1_8, ACC_PUBLIC | ACC_SUPER, "YumeCloudProtection/ThisApplicationIsProtectedByYumeCloud", null, "java/lang/Object", null);

        classWriter.visitSource("WhatIs.java", null);

        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            methodVisitor.visitInsn(RETURN);
            Label label1 = new Label();
            methodVisitor.visitLabel(label1);
            methodVisitor.visitLocalVariable("this", "Lcom/yumegod/obfuscator/loader/Loader;", null, label0, label1, 0);
            methodVisitor.visitMaxs(1, 1);
            methodVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC | ACC_NATIVE, "ProtectedByYumeCloud", "(ILjava/lang/Class;)V", "(ILjava/lang/Class<*>;)V", null);
            methodVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
            methodVisitor.visitCode();
            Label label0 = new Label();
            Label label1 = new Label();
            Label label2 = new Label();
            methodVisitor.visitTryCatchBlock(label0, label1, label2, "java/lang/Exception");
            methodVisitor.visitLabel(label0);
            methodVisitor.visitLdcInsn("YumeCloudProtection/YCVM"); // 1
            methodVisitor.visitVarInsn(ASTORE, 0); // 0

            methodVisitor.visitLdcInsn("YumeCloudProtection_"); // 1
            methodVisitor.visitLdcInsn(".YumeCloudProtection"); // 2
            methodVisitor.visitMethodInsn(INVOKESTATIC, "java/io/File", "createTempFile", "(Ljava/lang/String;Ljava/lang/String;)Ljava/io/File;", false); // 1
            methodVisitor.visitVarInsn(ASTORE, 1); // 0

            methodVisitor.visitVarInsn(ALOAD, 1); // 1
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/io/File", "deleteOnExit", "()V", false); // 0

            methodVisitor.visitLdcInsn(Type.getType("Lcom/yumegod/obfuscator/loader/Loader;")); // 1
            methodVisitor.visitTypeInsn(NEW, "java/lang/StringBuilder"); // 2
            methodVisitor.visitInsn(DUP); // 3
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false); // 2
            methodVisitor.visitLdcInsn("/"); // 3
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false); // 2
            methodVisitor.visitVarInsn(ALOAD, 0); // 3
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false); // 2
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false); // 2
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;", false); // 1
            methodVisitor.visitVarInsn(ASTORE, 2); // 0

            methodVisitor.visitVarInsn(ALOAD, 2); // 1
            Label label7 = new Label();
            methodVisitor.visitJumpInsn(IFNONNULL, label7); // 0

            methodVisitor.visitTypeInsn(NEW, "java/lang/Exception"); // 1
            methodVisitor.visitInsn(DUP); // 2
            methodVisitor.visitLdcInsn("YumeCloudProtection dependency not found!"); // 3
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Exception", "<init>", "(Ljava/lang/String;)V", false); // 1
            methodVisitor.visitInsn(ATHROW);
            methodVisitor.visitLabel(label7); // 0
            methodVisitor.visitFrame(Opcodes.F_APPEND, 3, new Object[]{"java/lang/String", "java/io/File", "java/io/InputStream"}, 0, null); // 0
            methodVisitor.visitVarInsn(ALOAD, 2); // 1
            methodVisitor.visitVarInsn(ALOAD, 1); // 2
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/io/File", "toPath", "()Ljava/nio/file/Path;", false); // 2
            methodVisitor.visitInsn(ICONST_1); // 3
            methodVisitor.visitTypeInsn(ANEWARRAY, "java/nio/file/CopyOption"); // 3
            methodVisitor.visitInsn(DUP); // 4
            methodVisitor.visitInsn(ICONST_0); // 5
            methodVisitor.visitFieldInsn(GETSTATIC, "java/nio/file/StandardCopyOption", "REPLACE_EXISTING", "Ljava/nio/file/StandardCopyOption;"); // 6
            methodVisitor.visitInsn(AASTORE); // 3
            methodVisitor.visitMethodInsn(INVOKESTATIC, "java/nio/file/Files", "copy", "(Ljava/io/InputStream;Ljava/nio/file/Path;[Ljava/nio/file/CopyOption;)J", false); // 2
            methodVisitor.visitInsn(POP2); // 0
            methodVisitor.visitVarInsn(ALOAD, 2); // 1
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/io/InputStream", "close", "()V", true); // 0

            methodVisitor.visitVarInsn(ALOAD, 1); //
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/io/File", "getAbsolutePath", "()Ljava/lang/String;", false);
            methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/System", "load", "(Ljava/lang/String;)V", false);
            methodVisitor.visitLabel(label1);
            Label label10 = new Label();
            methodVisitor.visitJumpInsn(GOTO, label10);
            methodVisitor.visitLabel(label2);
            methodVisitor.visitFrame(Opcodes.F_FULL, 0, new Object[]{}, 1, new Object[]{"java/lang/Exception"});
            methodVisitor.visitVarInsn(ASTORE, 0);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Exception", "printStackTrace", "()V", false);
            methodVisitor.visitLabel(label10);
            methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            methodVisitor.visitInsn(RETURN);
            methodVisitor.visitMaxs(6, 3);
            methodVisitor.visitEnd();
        }
        classWriter.visitEnd();

        return classWriter;
    }
}
