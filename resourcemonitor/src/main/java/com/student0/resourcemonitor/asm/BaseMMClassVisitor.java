package com.student0.resourcemonitor.asm;


import com.student0.resourcemonitor.config.NodeManager;

import org.objectweb.asm.ClassVisitor;

public class BaseMMClassVisitor extends ClassVisitor {

    protected static final String METHOD_NAME = "getResources";
    protected static final String METHOD_SIGN = "()Landroid/content/res/Resources;";

    private boolean mIsTargetClass = false;
    protected String mSuperClassName;
    protected String mClassName;

    public BaseMMClassVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        initTargetClass(name);
        super.visit(version, access, name, signature, superName, interfaces);
    }

    public boolean isTargetClass() {
        return mIsTargetClass;
    }

    private void initTargetClass(String className){
        String classType = className.replace("/", ".");
        mIsTargetClass =  NodeManager.Instance.removeContext(classType);
        if (mIsTargetClass){
            System.out.println(">>>" + className);
        }
    }
}
