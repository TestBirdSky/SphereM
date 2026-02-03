package com.sphere.shortvideos

import org.junit.Test
import java.io.File
import java.util.Base64
import java.nio.charset.StandardCharsets

/**
 * Risk.json 加密工具测试类
 *
 * 使用方法：
 * 1. 在 Android Studio 中打开此文件
 * 2. 点击 encryptRiskJson() 方法左侧的绿色运行按钮 ▶️
 * 3. 或者右键点击方法名，选择 "Run 'encryptRiskJson()'"
 * 4. 查看控制台输出的加密结果
 *
 * 修改配置：
 * - code: 异或加密密钥（默认 123）
 */
class EncryptRiskJsonTest {

    @Test
    fun encryptRiskJson() {
        // ========== 配置参数 ==========
        val code = 123  // 异或加密密钥，可以根据需要修改
        // =============================

        // 查找 risk.json 文件
        val currentDir = File("").absolutePath
        val possiblePaths = mutableListOf<String>()

        // 尝试多个可能的路径
        possiblePaths.add("app/risk.json")  // 从项目根目录
        possiblePaths.add("risk.json")  // 从 app 目录
        possiblePaths.add("../app/risk.json")  // 从其他目录

        // 从当前目录向上查找项目根目录
        var searchDir = File(currentDir)
        repeat(5) {
            val riskFile = File(searchDir, "app/risk.json")
            if (riskFile.exists()) {
                possiblePaths.add(riskFile.absolutePath)
            }
            searchDir = searchDir.parentFile ?: return@repeat
        }

        // 执行加密
        var success = false
        for (path in possiblePaths) {
            val file = File(path)
            if (file.exists()) {
                doEncrypt(file.absolutePath, code)
                success = true
                break
            }
        }

        if (!success) {
            println("❌ 错误: 无法找到 risk.json 文件")
            println("当前工作目录: $currentDir")
            println("\n已尝试的路径:")
            possiblePaths.forEach { path ->
                val file = File(path)
                println("  - $path ${if (file.exists()) "✅" else "❌"}")
            }
            println("\n💡 提示: 请确保 risk.json 文件位于 app/risk.json")
        }
    }

    /**
     * 加密方法：异或 + Base64 编码
     */
    private fun encrypt(data: String, code: Int): String {
        val bytes = data.toByteArray(StandardCharsets.UTF_8)

        // 异或加密
        val xorList = ByteArray(bytes.size)
        for (i in bytes.indices) {
            xorList[i] = (bytes[i].toInt() xor code).toByte()
        }

        // Base64 编码
        return Base64.getEncoder().encodeToString(xorList)
    }

    /**
     * 读取 risk.json 文件并加密
     */
    private fun doEncrypt(filePath: String, code: Int) {
        try {
            val file = File(filePath)
            val jsonContent = file.readText(StandardCharsets.UTF_8)
            val encrypted = encrypt(jsonContent, code)

            println("\n" + "=".repeat(80))
            println("✅ Risk.json 加密成功")
            println("=".repeat(80))
            println("文件路径: ${file.absolutePath}")
            println("原始内容长度: ${jsonContent.length} 字符")
            println("加密密钥 (code): $code")
            println("-".repeat(80))
            println("📋 加密后的字符串:")
            println()
            println(encrypted)
            println()
            println("-".repeat(80))
            println("=".repeat(80))
            println()

        } catch (e: Exception) {
            println("❌ 加密失败: ${e.message}")
            e.printStackTrace()
        }
    }

    @Test
    fun test2() {
        val st=
            "AHFbW1kOEllBWwBxW1tbW1kVDhYZHglZQVtLV3FbW1tbWRkeExoNEhQJWUFbS1dxW1tbW1kfHg0SGB5ZQVtLcVtbBldxW1tZGR4TGg0SFAlZQVsAcVtbW1tZGh8kCBMUCQ8kCBMUDFlBWwBxW1tbW1tbWR8OCRoPEhQVWUFbSEtXcVtbW1tbW1kNGhcOHllBW0hxW1tbWwZXcVtbW1tZGh8kCBMUCQ8kGBcUCB5ZQVsAcVtbW1tbW1kfDgkaDxIUFVlBW0lLV3FbW1tbW1tZDRoXDh5ZQVtIcVtbW1sGV3FbW1tbWQwJFBUcJB8eHhYkGh8kFx4ICFlBW0hXcVtbW1tZDAkUFRwkHx4eFiQaHyQWFAkeWUFbQktXcVtbW1tZFRQkEhUIDxoXF1lBW0pXcVtbW1tZGh8kHxoSFwIkCBMUDFlBW01LcVtbBldxW1tZHx4NEhgeWUFbIHFbW1tbWQ0LFVlXcVtbW1tZCRQUD1lXcVtbW1tZCBIWWVdxW1tbW1kIEhYOFxoPFAlZV3FbW1tbWRwUFBwXHgsXGgJZV3FbW1tbWR8eDR4XFAseCVlXcVtbW1tZEgtZcVtbJnEG"
        val result=decrypt(st,123)
        println("result:= $result")
    }

    private fun decrypt(data: String, code: Int): String { // Base64 解码
        val decodedBytes = Base64.getDecoder().decode(data)

        // 异或解密
        val xorList = ByteArray(decodedBytes.size)
        for (i in decodedBytes.indices) {
            xorList[i] = (decodedBytes[i].toInt() xor code).toByte()
        }

        // 转换为字符串
        return String(xorList, StandardCharsets.UTF_8)
    }
}
