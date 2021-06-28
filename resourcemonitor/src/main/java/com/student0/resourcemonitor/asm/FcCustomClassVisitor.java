package com.student0.resourcemonitor.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.IFNONNULL;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.PUTFIELD;

/**
 * @createDate: 6/25/21
 * @author: 飞彻
 * @description: 将getResource方法重命名为TMP_METHOD_NAME
 */
public class FcCustomClassVisitor extends BaseMMClassVisitor {

    private static final String PROXY_FIELD_NAME = "_fc_proxy_custom_resource";
    private static final String TMP_METHOD_NAME = METHOD_NAME + "_tmp_";

    public String mCustomResourceType = "";

    private boolean mChangeGetResource = false;

    public FcCustomClassVisitor(int i, ClassVisitor classVisitor) {
        super(i, classVisitor);
    }

    public FcCustomClassVisitor setClassName(String className) {
        if (className != null) {
            mCustomResourceType = className.replace(".", "/");
        }
        return this;
    }

    /**
     * 1、如果getResources有被重写,修改getResources为getResource_tmp_并重写实现getResources
     * private Resources mCustomResource;
     * public Resources getResources() {
     * if (this.mCustomResource == null) {
     * this.mCustomResource = new CustomResource(this.getResources_tmp_());
     * }
     * <p>
     * return this.mCustomResource;
     * }
     * <p>
     * public Resources getResources_tmp_() {
     * XXXXXX
     * }
     * <p>
     * 2、如果getResources没有被重写，则在visitEnd中重写getResources()
     * public Resources getResources() {
     * if (this.mCustomResource == null) {
     * this.mCustomResource = new CustomResource(super.getResource());
     * }
     * <p>
     * return this.mCustomResource;
     * }
     */

    @Override
    public MethodVisitor visitMethod(int i, String s, String s1, String s2, String[] strings) {
        if (METHOD_NAME.equals(s) && METHOD_SIGN.equals(s1) && isTargetClass() && isCustomResourceExist()) {
            mChangeGetResource = true;
            //重命名getResources为getResources_tmp_
            return super.visitMethod(i, TMP_METHOD_NAME, s1, s2, strings);
        }
        return super.visitMethod(i, s, s1, s2, strings);
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        if (isTargetClass() && isCustomResourceExist()) {
            visitField(ACC_PRIVATE, PROXY_FIELD_NAME, "Landroid/content/res/Resources;", null, null);
            //生成getResource方法
            visitGetResource(!mChangeGetResource);
        }
        mChangeGetResource = false;
    }

    @Override
    public void visitSource(String source, String debug) {
        super.visitSource(source, debug);
        mChangeGetResource = false;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        mClassName = name;//android/NullActivity
        mSuperClassName = superName;
    }

    private void visitGetResource(boolean customSuper) {
        MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "getResources", METHOD_SIGN, null, null);
        mv.visitCode();
        Label l0 = new Label();
        mv.visitLabel(l0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, mClassName, PROXY_FIELD_NAME, "Landroid/content/res/Resources;");
        Label l1 = new Label();
        mv.visitJumpInsn(IFNONNULL, l1);
        Label l2 = new Label();
        mv.visitLabel(l2);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitTypeInsn(NEW, mCustomResourceType);
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 0);
        if (customSuper) {
            mv.visitMethodInsn(INVOKESPECIAL, mSuperClassName, METHOD_NAME, METHOD_SIGN, false);
        } else {
            mv.visitMethodInsn(INVOKEVIRTUAL, mClassName, TMP_METHOD_NAME, METHOD_SIGN, false);
        }
        mv.visitMethodInsn(INVOKESPECIAL, mCustomResourceType, "<init>", "(Landroid/content/res/Resources;)V", false);
        mv.visitFieldInsn(PUTFIELD, mClassName, PROXY_FIELD_NAME, "Landroid/content/res/Resources;");
        mv.visitLabel(l1);
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, mClassName, PROXY_FIELD_NAME, "Landroid/content/res/Resources;");
        mv.visitInsn(ARETURN);
        Label l3 = new Label();
        mv.visitLabel(l3);
        mv.visitLocalVariable("this", "L" + mClassName + ";", null, l0, l3, 0);
        mv.visitMaxs(4, 1);
        mv.visitEnd();
    }

    private boolean isCustomResourceExist() {
        return mCustomResourceType != null && mCustomResourceType.length() > 0;
    }
}
