package com.onewaveinc.mrc;

public class LicenseException extends ModuleException {

    public static enum Code {

        /** 错误的类文件 */
        BAD_CLASS,
        
        /** 错误的硬件特征码 */
        BAD_HARDWARE_SIGNATURE,

        /** 错误的许可证信息 */
        BAD_LICENSE_DATA,

        /** 错误的许可证信息长度 */
        BAD_LICENSE_DATA_LENGTH,

        /** 错误的许可证功能模块 */
        BAD_LICENSE_MODULE,
        
        /** 许可证已过期 */
        EXPIRED,
        
        /** 找不到过期时间 */
        MISSING_EXPIRED_TIME,

        /** 找不到许可证信息 */
        MISSING_LICENSE_DATA,

        /** 找不到许可证文件 */
        MISSING_LICENSE_FILE,

        /** 找不到许可证功能模块 */
        MISSING_LICENSE_MODULE,
        
        /** 找不到MANIFEST.MF文件中需要的属性 */
        MISSING_MANIFEST_ATTRIBUTE,
        
        /** 找不到需要签名的模块列表文件 */
        MISSING_SIGNATURE_MODULE_FILE,

    }

    private static final long serialVersionUID = 1L;
    
    private Code code;

    public LicenseException(Code code) {
        this("MRC 未授权，代码: " + code.ordinal());
        this.code = code;
    }

    public LicenseException(String message) {
        super(message);
    }

    public Code getCode() {
        return code;
    }

}
