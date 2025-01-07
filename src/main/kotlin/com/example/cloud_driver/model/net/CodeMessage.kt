package com.example.cloud_driver.model.net

enum class CodeMessage(var code: String, var message: String) {
    OK("0000", ""),
    TOKEN_ILLEGAL("0001", "非法token"),
    TOKEN_TIMEOUT("0002", "token过期"),
    UN_OR_PW_ERROR("0003", "账号或密码错误"),
    CREATE_DIR_FAIL("0004", "创建文件夹目录失败"),
    DUPLICATE_UPLOAD_TASK("0005", "请勿重复上传"),
    DIR_ALREADY_EXIST("0006", "文件夹目录已存在"),
    DIR_OR_FILE_ALREADY_EXIST("0007", "文件或目录已存在"),
    RENAME_FILE_FAIL("0008", "文件或目录改名失败"),
    DIR_OR_FILE_NOT_EXIST("0008", "文件或目录不存在"),
}