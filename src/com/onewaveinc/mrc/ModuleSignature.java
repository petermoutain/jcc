package com.onewaveinc.mrc;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

import sun.misc.BASE64Encoder;

import com.onewaveinc.mrc.LicenseException.Code;

/**
 * 模块签名
 * @author gmice
 */
public class ModuleSignature {

    private static Logger logger = Logger.getLogger(ModuleSignature.class.getName());

    private static ModuleContext moduleContext;

    private MessageDigest md5 = null;

    private byte[] md5Digest;

    private Module module;

    private MessageDigest sha1 = null;

    private byte[] sha1Digest;

    public ModuleSignature(Module module) {
        this.module = module;
        digest();
    }

    public byte[] getMD5Digest() {
        return md5Digest;
    }

    public Module getModule() {
        return module;
    }

    public String getModuleId() {
        return module.getId();
    }

    public Version getModuleVersion() {
        return module.getVersion();
    }

    public byte[] getSHA1Digest() {
        return sha1Digest;
    }

    public void validate(byte[] signatureData) throws LicenseException {
        InputStream in = new ByteArrayInputStream(signatureData);

        try {
            // 读取开始两个字节（签名正文长度）
            int b0 = in.read();
            int b1 = in.read();
            if (b0 == -1 || b1 == -1) {
                throw new LicenseException(Code.BAD_LICENSE_DATA_LENGTH);
            }

            DataInputStream headerIn = new DataInputStream(new ByteArrayInputStream(signatureData, 2, b0 + (b1 << 8)));

            String moduleId = headerIn.readUTF();
            if (!moduleId.equals(module.getId())) {
                throw new LicenseException(Code.BAD_LICENSE_MODULE);
            }

            String moduleVersion = headerIn.readUTF();
            if (!moduleVersion.equals(module.getVersion().toString())) {
                throw new LicenseException(Code.BAD_LICENSE_MODULE);
            }

            byte[] md5Digest = new byte[16];
            headerIn.readFully(md5Digest);
            if (!Arrays.equals(md5Digest, this.md5Digest)) {
                throw new LicenseException(Code.BAD_LICENSE_MODULE);
            }

            byte[] sha1Digest = new byte[20];
            headerIn.readFully(sha1Digest);
            if (!Arrays.equals(sha1Digest, this.sha1Digest)) {
                throw new LicenseException(Code.BAD_LICENSE_MODULE);
            }
        } catch (EOFException e) {
            throw new LicenseException(Code.BAD_LICENSE_DATA_LENGTH);
        } catch (IOException e) {
            throw new LicenseException("未知错误：" + e.getMessage());
        }
    }

    private void digest() {
        try {
            md5 = MessageDigest.getInstance("MD5");
            sha1 = MessageDigest.getInstance("SHA-1");

            ModuleFile binDirectory = module.getModuleFile("/bin");
            if (binDirectory.isDirectory()) {
                digest(binDirectory);
            }

            ModuleFile libDirectory = module.getModuleFile("/lib");
            if (libDirectory.isDirectory()) {
                digest(libDirectory);
            }

            ModuleFile metainfDirectory = module.getModuleFile("/META-INF");
            if (metainfDirectory.isDirectory()) {
                digest(metainfDirectory);
            }

            md5Digest = md5.digest();
            sha1Digest = sha1.digest();
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    private void digest(byte[] input) {
        md5.update(input);
        sha1.update(input);
    }

    private void digest(byte[] input, int offset, int len) {
        md5.update(input, offset, len);
        sha1.update(input, offset, len);
    }

    private void digest(ModuleFile directory) throws Exception {
        ModuleFile[] files = directory.listFiles();

        Arrays.sort(files, new Comparator<File>() {
            public int compare(File o1, File o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        final byte[] buf = new byte[8192];
        for (ModuleFile file : files) {
            String modulePath = file.getModulePath();
            
            // 过滤一些文件
            if (modulePath.endsWith("-sources.jar")) continue; // TODO: 应该去掉此句
            if (modulePath.startsWith("/META-INF/maven/")) continue;
            if (modulePath.equals("/META-INF/MANIFEST.MF")) continue;
            
            if (file.isDirectory()) {
                digest(file);
            } else {
                digest(modulePath.getBytes("UTF-8"));
                
                FileInputStream in = new FileInputStream(file);
                int len;
                while ((len = in.read(buf)) != -1) {
                    digest(buf, 0, len);
                }
                in.close();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args == null || args.length < 1) return;

        String moduleId = args[0];
        Module module = null;
        if (moduleContext != null) {
            module = moduleContext.getModule(moduleId);
        }

        if (module == null) {
            module = Module.load(new File(moduleId));
            if (module == null) {
                logger.severe("待签名的模块不存在: " + moduleId);
                System.exit(1);
            }
        }

        ModuleSignature signature = new ModuleSignature(module);

        logger.info("模块 ID: " + signature.getModuleId());
        logger.info("模块版本: " + signature.getModuleVersion());
        logger.info("MD5: " + encode(signature.getMD5Digest()));
        logger.info("SHA-1: " + encode(signature.getSHA1Digest()));

        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.putValue("Module-ID", module.getId());
        attributes.putValue("Module-Version", module.getVersion().toString());
        attributes.putValue("Module-MD5", encode(signature.getMD5Digest()));
        attributes.putValue("Module-SHA1", encode(signature.getSHA1Digest()));

        FileOutputStream out = null;
        try {
            out = new FileOutputStream("signature.mf");
            manifest.write(out);
        } finally {
            if (out != null) out.close();
        }
    }

    public static void setModuleContext(ModuleContext moduleContext) {
        ModuleSignature.moduleContext = moduleContext;
    }

    private static String encode(byte[] data) {
        return new BASE64Encoder().encode(data).replaceAll("\r\n?", "");
    }

}
