package top.jessi.jhelper.file

import android.util.Log
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.nio.charset.Charset
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by Jessi on 2026/6/9 10:21
 * Email：17324719944@189.cn
 * Describe：CSV操作类（支持Java调用）
 */
object Csv {

    private const val TAG = "JHelper-Csv"

    /**
     * 文件锁，保证同一文件路径同一时间只被一个线程操作
     * key=文件绝对路径, value=锁对象
     */
    private val fileLocks = ConcurrentHashMap<String, Any>()

    /**
     * 在文件锁内执行操作，避免同一文件并发读写冲突
     */
    private inline fun <T> withFileLock(filePath: String, block: () -> T): T {
        val lock = fileLocks.getOrPut(filePath.intern()) { Any() }
        return synchronized(lock) {
            try {
                block()
            } finally {
                // 清理不再使用的锁对象，防止内存泄漏
                fileLocks.remove(filePath.intern(), lock)
            }
        }
    }

    /**
     * 读取CSV文件
     * @param filePath 文件路径
     * @param charset 字符编码，默认UTF-8
     * @return CSV数据，失败返回空列表
     */
    @JvmStatic
    @JvmOverloads
    fun read(filePath: String, charset: Charset = Charsets.UTF_8): List<List<String>> {
        val data = mutableListOf<List<String>>()
        try {
            BufferedReader(InputStreamReader(FileInputStream(filePath), charset)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val row = parseLine(line!!).toList()
                    data.add(row)
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "读取CSV文件失败: $filePath", e)
        }
        return data
    }

    /**
     * 读取CSV文件（带头部）
     * @param filePath 文件路径
     * @param charset 字符编码，默认UTF-8
     * @return CSV数据（每行是列名到值的映射），失败返回空列表
     */
    @JvmStatic
    @JvmOverloads
    fun readWithHeader(filePath: String, charset: Charset = Charsets.UTF_8): List<Map<String, String>> {
        val data = mutableListOf<Map<String, String>>()
        try {
            BufferedReader(InputStreamReader(FileInputStream(filePath), charset)).use { reader ->
                // 读取头部
                val headerLine = reader.readLine() ?: return emptyList()
                val headers = parseLine(headerLine).toList()
                // 读取数据行
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val values = parseLine(line!!).toList()
                    val rowMap = mutableMapOf<String, String>()
                    for (i in headers.indices) {
                        if (i < values.size) {
                            rowMap[headers[i]] = values[i]
                        }
                    }
                    data.add(rowMap)
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "读取CSV文件失败: $filePath", e)
        }
        return data
    }

    /**
     * 写入CSV文件
     * @param filePath 文件路径
     * @param data CSV数据
     * @param charset 字符编码，默认UTF-8
     * @param append 是否追加模式，默认false（覆盖）
     * @param deleteBackup 成功后是否删除备份文件，默认true（删除）
     */
    @JvmStatic
    @JvmOverloads
    fun write(
        filePath: String, data: List<List<String>>,
        charset: Charset = Charsets.UTF_8, append: Boolean = false, deleteBackup: Boolean = true
    ) {
        withFileLock(filePath) {
            if (append) {
                // 追加模式：直接写入目标文件 + sync
                val fos = FileOutputStream(filePath, true)
                try {
                    PrintWriter(OutputStreamWriter(fos, charset)).use { writer ->
                        for (row in data) {
                            writer.println(joinLine(row.toMutableList()))
                        }
                        writer.flush()
                        fos.fd.sync()
                    }
                } catch (e: IOException) {
                    Log.w(TAG, "写入CSV文件失败: $filePath", e)
                }
                return@withFileLock
            }
            // 覆盖模式：写入临时文件 + sync + 备份式替换（防止清空）
            val inputFile = File(filePath)
            val tempFile = File("$filePath.tmp")
            val fos = FileOutputStream(tempFile)
            try {
                PrintWriter(OutputStreamWriter(fos, charset)).use { writer ->
                    for (row in data) {
                        writer.println(joinLine(row.toMutableList()))
                    }
                    writer.flush()
                    fos.fd.sync()
                }
            } catch (e: IOException) {
                Log.w(TAG, "写入CSV文件失败: $filePath", e)
                tempFile.delete()
                return@withFileLock
            }
            val backupFile = File("$filePath.bak")
            backupFile.delete()
            if (!inputFile.renameTo(backupFile)) {
                Log.w(TAG, "无法创建备份，操作中止，原文件未修改: $filePath")
                tempFile.delete()
                return@withFileLock
            }
            if (!tempFile.renameTo(inputFile)) {
                Log.w(TAG, "替换失败，正在从备份恢复: $filePath")
                backupFile.renameTo(inputFile)
                tempFile.delete()
                return@withFileLock
            }
            // 默认删除备份，用户可选择保留备份文件
            if (deleteBackup) backupFile.delete()
        }
    }

    /**
     * 追加单行数据到CSV文件
     * @param filePath 文件路径
     * @param row 单行数据
     * @param charset 字符编码，默认UTF-8
     */
    @JvmStatic
    @JvmOverloads
    fun appendLine(filePath: String, row: List<String>, charset: Charset = Charsets.UTF_8) {
        withFileLock(filePath) {
            val fos = FileOutputStream(filePath, true)
            try {
                PrintWriter(OutputStreamWriter(fos, charset)).use { writer ->
                    writer.println(joinLine(row.toMutableList()))
                    writer.flush()
                    fos.fd.sync()
                }
            } catch (e: IOException) {
                Log.w(TAG, "追加CSV行失败: $filePath", e)
            }
        }
    }

    /**
     * 根据条件过滤CSV数据
     * @param data CSV数据
     * @param columnIndex 列索引
     * @param value 匹配值
     * @return 过滤后的数据
     */
    @JvmStatic
    fun filter(data: List<List<String>>, columnIndex: Int, value: String): List<List<String>> {
        require(columnIndex >= 0) { "列索引不能为负数" }
        return data.filter { row ->
            row.size > columnIndex && row[columnIndex] == value
        }
    }

    /**
     * 根据条件过滤CSV数据（直接操作文件）
     * @param filePath 文件路径
     * @param columnIndex 列索引
     * @param value 匹配值
     * @param charset 字符编码，默认UTF-8
     * @return 过滤后的数据
     */
    @JvmStatic
    @JvmOverloads
    fun filter(
        filePath: String, columnIndex: Int, value: String, charset: Charset = Charsets.UTF_8
    ): List<List<String>> {
        require(columnIndex >= 0) { "列索引不能为负数" }
        val allData = read(filePath, charset)
        return filter(allData, columnIndex, value)
    }

    /**
     * 获取指定列的所有值
     * @param data CSV数据
     * @param columnIndex 列索引
     * @return 列值列表
     */
    @JvmStatic
    fun getColumnValues(data: List<List<String>>, columnIndex: Int): List<String> {
        require(columnIndex >= 0) { "列索引不能为负数" }
        return data.mapNotNull { row ->
            if (row.size > columnIndex) row[columnIndex] else null
        }
    }

    /**
     * 获取指定列的所有值（直接操作文件）
     * @param filePath 文件路径
     * @param columnIndex 列索引
     * @param charset 字符编码，默认UTF-8
     * @return 列值列表
     */
    @JvmStatic
    @JvmOverloads
    fun getColumnValues(filePath: String, columnIndex: Int, charset: Charset = Charsets.UTF_8): List<String> {
        require(columnIndex >= 0) { "列索引不能为负数" }
        val allData = read(filePath, charset)
        return getColumnValues(allData, columnIndex)
    }

    /**
     * 修改指定单元格（直接操作文件）
     * @param filePath 文件路径
     * @param rowIndex 行索引
     * @param colIndex 列索引
     * @param newValue 新值
     * @param charset 字符编码，默认UTF-8
     * @param deleteBackup 成功后是否删除备份文件，默认true（删除）
     * @return 是否成功修改
     */
    @JvmStatic
    @JvmOverloads
    fun updateCell(
        filePath: String, rowIndex: Int, colIndex: Int, newValue: String,
        charset: Charset = Charsets.UTF_8, deleteBackup: Boolean = true
    ): Boolean {
        require(rowIndex >= 0) { "行索引不能为负数" }
        require(colIndex >= 0) { "列索引不能为负数" }
        return withFileLock(filePath) {
            val inputFile = File(filePath)
            val tempFile = File("$filePath.tmp")
            var updated = false
            try {
                BufferedReader(InputStreamReader(FileInputStream(inputFile), charset)).use { reader ->
                    BufferedWriter(OutputStreamWriter(FileOutputStream(tempFile), charset)).use { writer ->
                        var line: String?
                        var currentRow = 0
                        while (reader.readLine().also { line = it } != null) {
                            if (currentRow == rowIndex) {
                                val row = parseLine(line!!)
                                if (colIndex < row.size) {
                                    row[colIndex] = newValue
                                    updated = true
                                }
                                writer.write(joinLine(row))
                            } else {
                                writer.write(line)
                            }
                            writer.newLine()
                            currentRow++
                        }
                    }
                }
            } catch (e: IOException) {
                Log.w(TAG, "修改CSV单元格失败: $filePath", e)
                tempFile.delete()
                return@withFileLock false
            }

            // 用备份式替换（兼容minSdk 21，任何失败可恢复）
            val backupFile = File("$filePath.bak")
            backupFile.delete()
            if (!inputFile.renameTo(backupFile)) {
                Log.w(TAG, "无法创建备份，操作中止，原文件未修改: $filePath")
                tempFile.delete()
                return@withFileLock false
            }
            if (!tempFile.renameTo(inputFile)) {
                Log.w(TAG, "替换失败，正在从备份恢复: $filePath")
                backupFile.renameTo(inputFile)
                tempFile.delete()
                return@withFileLock false
            }
            // 默认删除备份，用户可选择保留备份文件
            if (deleteBackup) backupFile.delete()
            updated
        }
    }

    /**
     * 更新指定条件的行（直接操作文件）
     * @param filePath 文件路径
     * @param conditionColumn 条件列索引
     * @param conditionValue 条件值
     * @param updateColumn 更新列索引
     * @param newValue 新值
     * @param charset 字符编码，默认UTF-8
     * @param deleteBackup 成功后是否删除备份文件，默认true（删除）
     * @return 是否成功修改
     */
    @JvmStatic
    @JvmOverloads
    fun updateRowByCondition(
        filePath: String, conditionColumn: Int, conditionValue: String,
        updateColumn: Int, newValue: String, charset: Charset = Charsets.UTF_8, deleteBackup: Boolean = true
    ): Boolean {
        require(conditionColumn >= 0) { "条件列索引不能为负数" }
        require(updateColumn >= 0) { "更新列索引不能为负数" }
        return withFileLock(filePath) {
            val inputFile = File(filePath)
            val tempFile = File("$filePath.tmp")
            var updated = false
            try {
                BufferedReader(InputStreamReader(FileInputStream(inputFile), charset)).use { reader ->
                    BufferedWriter(OutputStreamWriter(FileOutputStream(tempFile), charset)).use { writer ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val row = parseLine(line!!)
                            if (row.size > conditionColumn && row[conditionColumn] == conditionValue) {
                                if (updateColumn < row.size) {
                                    row[updateColumn] = newValue
                                    updated = true
                                }
                                writer.write(joinLine(row))
                            } else {
                                writer.write(line)
                            }
                            writer.newLine()
                        }
                    }
                }
            } catch (e: IOException) {
                Log.w(TAG, "更新CSV行失败: $filePath", e)
                tempFile.delete()
                return@withFileLock false
            }

            // 用备份式替换（兼容minSdk 21，任何失败可恢复）
            val backupFile = File("$filePath.bak")
            backupFile.delete()
            if (!inputFile.renameTo(backupFile)) {
                Log.w(TAG, "无法创建备份，操作中止，原文件未修改: $filePath")
                tempFile.delete()
                return@withFileLock false
            }
            if (!tempFile.renameTo(inputFile)) {
                Log.w(TAG, "替换失败，正在从备份恢复: $filePath")
                backupFile.renameTo(inputFile)
                tempFile.delete()
                return@withFileLock false
            }
            // 默认删除备份，用户可选择保留备份文件
            if (deleteBackup) backupFile.delete()
            updated
        }
    }

    /**
     * 解析CSV行（处理引号）
     */
    private fun parseLine(line: String): MutableList<String> {
        val result = mutableListOf<String>()
        var inQuotes = false
        val current = StringBuilder()
        for (c in line.toCharArray()) {
            when {
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(c)
            }
        }
        result.add(current.toString())
        return result
    }

    /**
     * 拼接CSV行
     */
    private fun joinLine(row: MutableList<String>): String {
        return buildString {
            for (i in row.indices) {
                if (i > 0) append(",")
                // 如果包含逗号、引号或换行符，用引号包裹
                if (row[i].contains(",") || row[i].contains("\"") || row[i].contains("\n") || row[i].contains("\r")) {
                    append("\"").append(row[i].replace("\"", "\"\"")).append("\"")
                } else {
                    append(row[i])
                }
            }
        }
    }
}