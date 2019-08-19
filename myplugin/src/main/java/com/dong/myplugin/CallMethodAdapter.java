package com.dong.myplugin;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Created by dongjiangpeng on 2019/8/16 0016.
 */
public class CallMethodAdapter extends MethodVisitor {


    public CallMethodAdapter(MethodVisitor mv) {
        super(Opcodes.ASM5, mv);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        if ("com/dong/plugin/MyLog".equals(owner)) {

        } else if ("com/dong/plugin/Test".equals(owner)) {
            mv.visitLdcInsn("dong hook CALL " + name);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "com/dong/plugin/MyLog", "log", "(Ljava/lang/String;)V", false);
            mv.visitMethodInsn(opcode, owner, name, desc, itf);
        } else {
            mv.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }
}
