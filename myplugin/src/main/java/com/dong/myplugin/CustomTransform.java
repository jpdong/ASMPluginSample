package com.dong.myplugin;

import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Status;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.api.transform.TransformOutputProvider;
import com.android.build.gradle.internal.pipeline.TransformManager;
import com.android.utils.FileUtils;

import org.apache.commons.io.IOUtils;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Created by dongjiangpeng on 2019/8/16 0016.
 */
public class CustomTransform extends Transform {

    private Logger logger;

    private static final FileTime ZERO = FileTime.fromMillis(0);

    public CustomTransform(Project project) {
        logger = project.getLogger();
    }

    @Override
    public String getName() {
        return "CustomTransform";
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
        return true;
    }

    @Override
    public Set<QualifiedContent.ContentType> getOutputTypes() {
        return super.getOutputTypes();
    }

    @Override
    public Set<? super QualifiedContent.Scope> getReferencedScopes() {
        return TransformManager.EMPTY_SCOPES;
    }

    @Override
    public Map<String, Object> getParameterInputs() {
        return super.getParameterInputs();
    }

    @Override
    public boolean isCacheable() {
        return true;
    }

    @Override
    public void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation);
        logger.warn("CustomTransform/transform");
        transformWork(transformInvocation);
    }

    private void transformWork(TransformInvocation transformInvocation) throws IOException {
        boolean isIncremental = transformInvocation.isIncremental();
        Collection<TransformInput> inputs = transformInvocation.getInputs();
        Collection<TransformInput> referencedInputs = transformInvocation.getReferencedInputs();
        TransformOutputProvider outputProvider = transformInvocation.getOutputProvider();
        isIncremental = false;
        if (isIncremental) {
            for (TransformInput input : inputs) {
                for (JarInput jarInput : input.getJarInputs()) {
                    logger.warn("CustomTransform/transform jar " + jarInput.getName());
                    Status status = jarInput.getStatus();
                    File dest = outputProvider.getContentLocation(jarInput.getFile().getAbsolutePath(), jarInput.getContentTypes(), jarInput.getScopes(), Format.JAR);
                    switch (status) {
                        case NOTCHANGED:
                            break;
                        case ADDED:
                        case CHANGED:
                            transformJar(jarInput.getFile(), dest, status);
                            break;
                        case REMOVED:
                            if (dest.exists()) {
                                FileUtils.delete(dest);
                            }
                            break;
                        default:
                            break;
                    }
                }
                for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                    File dest = outputProvider.getContentLocation(directoryInput.getFile().getAbsolutePath(), directoryInput.getContentTypes(), directoryInput.getScopes(), Format.DIRECTORY);
                    FileUtils.mkdirs(dest);
                    String srcDirPath = directoryInput.getFile().getAbsolutePath();
                    String destDirPath = dest.getAbsolutePath();
                    Map<File, Status> fileStatusMap = directoryInput.getChangedFiles();
                    for (Map.Entry<File, Status> changedFile : fileStatusMap.entrySet()) {
                        Status status = changedFile.getValue();
                        File inputFile = changedFile.getKey();
                        logger.warn("CustomTransform/transform class " + inputFile.getName());
                        String destFilePath = inputFile.getAbsolutePath().replace(srcDirPath, destDirPath);
                        File destFile = new File(destFilePath);
                        switch (status) {
                            case NOTCHANGED:
                                break;
                            case REMOVED:
                                if (destFile.exists()) {
                                    destFile.delete();
                                }
                                break;
                            case ADDED:
                            case CHANGED:
                                transformSingleFile(inputFile, destFile, srcDirPath);
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
        } else {
            outputProvider.deleteAll();
            for (TransformInput input : inputs) {
                for (JarInput jarInput : input.getJarInputs()) {
                    Status status = jarInput.getStatus();
                    File dest = outputProvider.getContentLocation(jarInput.getFile().getAbsolutePath(), jarInput.getContentTypes(), jarInput.getScopes(), Format.JAR);
                    logger.warn("copy jar to dest " + dest.getAbsolutePath());
                    org.apache.commons.io.FileUtils.touch(dest);
                    transformJar(jarInput.getFile(), dest, status);
                }
                for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                    String dirPath = directoryInput.getFile().getAbsolutePath();
                    File dest = outputProvider.getContentLocation(directoryInput.getFile().getAbsolutePath(), directoryInput.getContentTypes(), directoryInput.getScopes(), Format.DIRECTORY);
                    logger.warn(dest.getAbsolutePath() + " is exist " + dest.exists());
                    if (!dest.exists()) {
                        dest.mkdirs();
                    }
                    for (File file : FileUtils.getAllFiles(directoryInput.getFile())) {
                        String filePath = file.getAbsolutePath();
                        File outputFile = new File(filePath.replace(dirPath, dest.getAbsolutePath()));
                        logger.warn("copy class to dest " + outputFile.getAbsolutePath());
                        org.apache.commons.io.FileUtils.touch(outputFile);
                        transformSingleFile(file, outputFile, dirPath);
                    }
                }
            }
        }
    }


    private void transformSingleFile(File inputFile, File destFile, String srcDirPath) throws IOException {
        if (isTargetClass(inputFile.getAbsolutePath().replace(srcDirPath, "").replace("/", "."))) {
            copy(inputFile.getAbsolutePath(), destFile.getAbsolutePath());
        } else {
            logger.warn("is not target " + inputFile.getName());
            FileUtils.copyFile(inputFile, destFile);
        }
    }

    private void transformJar(File inputJar, File outputJar, Status status) throws IOException {
        ZipFile inputZip = new ZipFile(inputJar);
        ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(outputJar.toPath())));
        Enumeration<? extends ZipEntry> inputZipEntries = inputZip.entries();
        while (inputZipEntries.hasMoreElements()) {
            ZipEntry zipEntry = inputZipEntries.nextElement();
            InputStream originalFile = new BufferedInputStream(inputZip.getInputStream(zipEntry));
            ZipEntry outputEntry = new ZipEntry(zipEntry.getName());
            byte[] newEntryContent;
            if (isTargetClass(outputEntry.getName().replace("/", "."))) {
                newEntryContent = transformSingleClassToByteArray(originalFile);
            } else {
                newEntryContent = IOUtils.toByteArray(originalFile);
            }
            CRC32 crc32 = new CRC32();
            crc32.update(newEntryContent);
            outputEntry.setCrc(crc32.getValue());
            outputEntry.setMethod(ZipEntry.STORED);
            outputEntry.setSize(newEntryContent.length);
            outputEntry.setCompressedSize(newEntryContent.length);
            outputEntry.setLastAccessTime(ZERO);
            outputEntry.setLastModifiedTime(ZERO);
            outputEntry.setCreationTime(ZERO);
            zipOutputStream.putNextEntry(outputEntry);
            zipOutputStream.write(newEntryContent);
            zipOutputStream.closeEntry();
        }
        zipOutputStream.flush();
        zipOutputStream.close();
    }

    public byte[] transformSingleClassToByteArray(InputStream inputStream) throws IOException {
        ClassReader classReader = new ClassReader(inputStream);
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        CallClassAdapter adapter = new CallClassAdapter(classWriter);
        classReader.accept(classWriter, 0);
        return classWriter.toByteArray();
    }

    public boolean isTargetClass(String fullQualifiedClassName) {
        return fullQualifiedClassName.endsWith(".class") && !fullQualifiedClassName.contains("R$") && !fullQualifiedClassName.contains("R.class") && !fullQualifiedClassName.contains("BuildConfig.class");
    }

    private void copy(String inputPath, String outputPath) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(inputPath);
        ClassReader classReader = new ClassReader(fileInputStream);
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        CallClassAdapter adapter = new CallClassAdapter(classWriter);
        classReader.accept(adapter, 0);
        FileOutputStream fileOutputStream = new FileOutputStream(outputPath);
        fileOutputStream.write(classWriter.toByteArray());
        fileOutputStream.close();
    }
}
