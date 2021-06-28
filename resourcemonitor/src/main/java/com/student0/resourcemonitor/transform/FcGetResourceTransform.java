package com.student0.resourcemonitor.transform;

import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.api.transform.TransformOutputProvider;
import com.android.build.gradle.internal.pipeline.TransformManager;
import com.android.utils.FileUtils;
import com.student0.resourcemonitor.asm.FcCustomClassVisitor;
import com.student0.resourcemonitor.config.NodeManager;
import com.student0.resourcemonitor.config.ResourceMonitorExtension;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.utils.IOUtils;
import org.gradle.api.Project;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

/**
 * @createDate: 6/26/21
 * @author: 飞彻
 * @description:
 */
public class FcGetResourceTransform extends Transform {
    private Project mProject;
    private String mCustomResourceName;

    public FcGetResourceTransform(Project project) {
        mProject = project;
    }

    @Override
    public void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        //拿到所有的class文件
        mCustomResourceName = mProject.getExtensions().findByType(ResourceMonitorExtension.class).className;
        Collection<TransformInput> inputs = transformInvocation.getInputs();
        TransformOutputProvider outputProvider = transformInvocation.getOutputProvider();
        if (outputProvider != null) {
            outputProvider.deleteAll();
        }
        //遍历inputs Transform的inputs
        for (TransformInput input : inputs) {
            // 源码处理
            for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                handDirectoryInput(directoryInput, outputProvider);
            }
            //遍历jar包处理
            Iterator<JarInput> iterator = input.getJarInputs().iterator();
            while (iterator.hasNext()) {
                handJarInput(iterator.next(), outputProvider);
            }
        }
    }

    private void handDirectoryInput(DirectoryInput input, TransformOutputProvider outputProvider) throws IOException {
        readClassForFile(input.getFile());
        File dest = outputProvider.getContentLocation(input.getName(), input.getContentTypes(), input.getScopes(), Format.DIRECTORY);
        FileUtils.copyDirectory(input.getFile(), dest);
    }

    private void readClassForFile(File file) throws IOException {
        if (file.isDirectory())
            for (File fs : file.listFiles()) {
                readClassForFile(fs);
            }
        else {
            String[] strings = file.getPath().split("/classes/");
            if (strings == null || strings.length < 2) return;
            if (!isTargetFile(strings[1])) return;
            FileInputStream is = new FileInputStream(file);

            ClassReader cr = new ClassReader(is);
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            ClassVisitor classVisitor = new FcCustomClassVisitor(Opcodes.ASM4, cw)
                    .setClassName(mCustomResourceName);

            cr.accept(classVisitor, 0);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(cw.toByteArray());
            fos.close();
            is.close();
        }
    }

    //处理jar中的class
    private void handJarInput(JarInput jarInput, TransformOutputProvider outputProvider) throws IOException {
        if (jarInput.getFile().getAbsolutePath().endsWith(".jar")) {

            String jarName = jarInput.getName();
            String md5Name = DigestUtils.md5Hex(jarInput.getFile().getAbsolutePath());
            if (jarName.endsWith(".jar")) {
                jarName = jarName.substring(0, jarName.length() - 4);
            }
            JarFile jarFile = new JarFile(jarInput.getFile());
            Enumeration enumeration = jarFile.entries();
            File tmpFile = new File(jarInput.getFile().getParent() + File.separator + "classes_temp.jar");

            if (tmpFile.exists()) {
                tmpFile.delete();
            }
            JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(tmpFile));

            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) enumeration.nextElement();
                String entryName = jarEntry.getName();
                ZipEntry zipEntry = new ZipEntry(entryName);
                InputStream inputStream = jarFile.getInputStream(jarEntry);
                if (isTargetFile(entryName)) {
                    //class文件处理
                    jarOutputStream.putNextEntry(zipEntry);
                    ClassReader classReader = new ClassReader(IOUtils.toByteArray(inputStream));
                    ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
                    ClassVisitor cv = new FcCustomClassVisitor(Opcodes.ASM4, classWriter)
                            .setClassName(mCustomResourceName);
                    classReader.accept(cv, ClassReader.EXPAND_FRAMES);
                    byte[] code = classWriter.toByteArray();
                    jarOutputStream.write(code);
                } else {
                    jarOutputStream.putNextEntry(zipEntry);
                    jarOutputStream.write(IOUtils.toByteArray(inputStream));
                }
                jarOutputStream.closeEntry();
            }
            jarOutputStream.close();
            jarFile.close();

            File dest = outputProvider.getContentLocation(jarName + md5Name,
                    jarInput.getContentTypes(), jarInput.getScopes(), Format.JAR);
            FileUtils.copyFile(tmpFile, dest);
            tmpFile.delete();
        }
    }

    public static boolean isTargetFile(String fileName) {
        fileName = fileName.replace(".class", "");
        fileName = fileName.replace("/", ".");
        return NodeManager.Instance.containContext(fileName);
    }


    @Override
    public String getName() {
        return "fc_get_resource_transform";
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT;
    }

    @Override
    public boolean isIncremental() {
        return false;
    }
}
