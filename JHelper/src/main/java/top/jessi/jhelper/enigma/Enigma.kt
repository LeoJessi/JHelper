package top.jessi.jhelper.enigma

import android.text.TextUtils
import android.util.Base64
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.RSAKeyGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.util.Locale
import java.util.Random
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Created by Jessi on 2022/8/12 17:06
 * Email：17324719944@189.cn
 * Describe：加密解密算法
 */
object Enigma {

    @JvmStatic
    fun md5(plaintext: String): String {
        // 创建摘要算法对象
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(plaintext.toByteArray())
        // 转 16 进制并补零,再直接拼接字符串
        return digest.joinToString("") { "%02X".format(it) }
    }

    /********************************** CAESAR **********************************/
    @JvmStatic
    fun encryptCaesar(plaintext: String, key: Int): String {
        // 将字符串转换为数组
        val chars: CharArray = plaintext.toCharArray()
        val buffer = StringBuilder()
        // 遍历数组
        for (aChar in chars) {
            // 获取字符的ASCII编码
            var asciiCode = aChar.code
            // 偏移数据
            asciiCode += key
            // 将偏移后的数据转为字符
            val result = asciiCode.toChar()
            // 拼接数据
            buffer.append(result)
        }
        return buffer.toString()
    }

    @JvmStatic
    fun decryptCaesar(ciphertext: String, key: Int): String {
        // 将字符串转换为数组
        val chars: CharArray = ciphertext.toCharArray()
        val buffer = StringBuilder()
        // 遍历数组
        for (aChar in chars) {
            // 获取字符的ASCII编码
            var asciiCode = aChar.code
            // 偏移数据
            asciiCode -= key
            // 将偏移后的数据转为字符
            val result = asciiCode.toChar()
            // 拼接数据
            buffer.append(result)
        }
        return buffer.toString()
    }
    /********************************** CAESAR **********************************/

    /********************************** VIRGINIA **********************************/
    @JvmStatic
    fun encryptVirginia(plaintext: String, key: String): String {
        val result = StringBuilder()
        val keyLength = key.length
        var j = 0
        for (c in plaintext) {
            if (c.isLetter()) {
                val base = if (c.isUpperCase()) 'A' else 'a'
                val keyChar =
                    if (c.isUpperCase()) key[j % keyLength].uppercaseChar() else key[j % keyLength].lowercaseChar()
                val encrypted = ((c - base + (keyChar - base)) % 26 + base.code).toChar()
                result.append(encrypted)
                j++
            } else {
                result.append(c)
            }
        }
        return result.toString()
    }

    @JvmStatic
    fun decryptVirginia(ciphertext: String, key: String): String {
        val result = StringBuilder()
        val keyLength = key.length
        var j = 0
        for (c in ciphertext) {
            if (c.isLetter()) {
                val base = if (c.isUpperCase()) 'A' else 'a'
                val keyChar =
                    if (c.isUpperCase()) key[j % keyLength].uppercaseChar() else key[j % keyLength].lowercaseChar()
                val decrypted = ((c - base - (keyChar - base) + 26) % 26 + base.code).toChar()
                result.append(decrypted)
                j++
            } else {
                result.append(c)
            }
        }
        return result.toString()
    }
    /********************************** VIRGINIA **********************************/

    /********************************** FENCE **********************************/
    /**
     * 栅栏加密
     *
     * @param plaintext 明文
     * @param key 密钥 --> 不可超过明文长度
     * @return 密文
     */
    @JvmStatic
    fun encryptFence(plaintext: String, key: Int): String {
        require(key in 2..plaintext.length) { "Key must be >=2 and <= message length" }
        val rails = List(key) { StringBuilder() }
        var rail = 0
        var down = true
        for (c in plaintext) {
            rails[rail].append(c)
            if (rail == 0) down = true
            else if (rail == key - 1) down = false
            rail += if (down) 1 else -1
        }
        return rails.joinToString("") { it.toString() }
    }

    @JvmStatic
    fun decryptFence(ciphertext: String, key: Int): String {
        require(key in 2..ciphertext.length) { "Key must be >=2 and <= message length" }
        val len = ciphertext.length
        val railLens = IntArray(key) { 0 }
        // 1️⃣ 先计算每条轨道字符数量
        var rail = 0
        var down = true
        repeat(len) {
            railLens[rail]++
            if (rail == 0) down = true
            else if (rail == key - 1) down = false
            rail += if (down) 1 else -1
        }
        // 2️⃣ 将密文分配到各轨道
        val rails = Array(key) { StringBuilder() }
        var index = 0
        for (i in 0 until key) {
            val end = index + railLens[i]
            rails[i].append(ciphertext.substring(index, end))
            index = end
        }
        // 3️⃣ 按轨迹顺序读取明文
        val plaintext = StringBuilder()
        rail = 0
        down = true
        val railPos = IntArray(key) { 0 }
        repeat(len) {
            plaintext.append(rails[rail][railPos[rail]])
            railPos[rail]++
            if (rail == 0) down = true
            else if (rail == key - 1) down = false
            rail += if (down) 1 else -1
        }
        return plaintext.toString()
    }
    /********************************** FENCE **********************************/

    /********************************** SHA **********************************/
    @JvmStatic
    fun sha1(plaintext: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        val digest = md.digest(plaintext.toByteArray())
        // 转 16 进制并补零,再直接拼接字符串
        return digest.joinToString("") { "%02X".format(it) }
    }

    @JvmStatic
    fun sha256(plaintext: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(plaintext.toByteArray())
        // 转 16 进制并补零,再直接拼接字符串
        return digest.joinToString("") { "%02X".format(it) }
    }

    @JvmStatic
    fun sha384(plaintext: String): String {
        val md = MessageDigest.getInstance("SHA-384")
        val digest = md.digest(plaintext.toByteArray())
        // 转 16 进制并补零,再直接拼接字符串
        return digest.joinToString("") { "%02X".format(it) }
    }

    @JvmStatic
    fun sha512(plaintext: String): String {
        val md = MessageDigest.getInstance("SHA-512")
        val digest = md.digest(plaintext.toByteArray())
        // 转 16 进制并补零,再直接拼接字符串
        return digest.joinToString("") { "%02X".format(it) }
    }
    /********************************** SHA **********************************/

    /********************************** AES **********************************/
    /**
     * AES加密(结果为字节数组)
     *
     * @param plaintext 明文
     * @param key       密钥
     * @param iv        偏移量
     * @return 密文
     */
    @JvmStatic
    fun encryptAes(plaintext: ByteArray, key: String, iv: String): ByteArray {
        return try {
            if (plaintext.isEmpty()) throw IllegalArgumentException("The plaintext cannot be empty.")
            checkAesKeyAndIv(key, iv)
            // 如果数据安全性要求很高，考虑改用 AES/GCM/NoPadding，它自带认证（防篡改）
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val keySpec = SecretKeySpec(key.toByteArray(StandardCharsets.UTF_8), "AES")
            val ivSpec = IvParameterSpec(iv.toByteArray(StandardCharsets.UTF_8))
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            cipher.doFinal(plaintext)
        } catch (e: Exception) {
            e.printStackTrace()
            ByteArray(0)
        }
    }

    /**
     * AES加密(结果为16进制字符串)
     *
     * @param plaintext 明文
     * @param key       密钥
     * @param iv        偏移量
     * @return 密文
     */
    @JvmStatic
    fun encryptAes(plaintext: String, key: String, iv: String): String {
        return try {
            if (TextUtils.isEmpty(plaintext)) throw IllegalArgumentException("The plaintext cannot be empty.")
            val bytes = encryptAes(plaintext.toByteArray(StandardCharsets.UTF_8), key, iv)
            return if (bytes.isEmpty()) "" else byteToHex(bytes)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * AES解密(结果为字节数组)
     *
     * @param ciphertext 明文
     * @param key       密钥
     * @param iv        偏移量
     * @return 密文
     */
    @JvmStatic
    fun decryptAes(ciphertext: ByteArray, key: String, iv: String): ByteArray {
        return try {
            if (ciphertext.isEmpty()) throw IllegalArgumentException("The ciphertext cannot be empty.")
            checkAesKeyAndIv(key, iv)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val keySpec = SecretKeySpec(key.toByteArray(StandardCharsets.UTF_8), "AES")
            val ivSpec = IvParameterSpec(iv.toByteArray(StandardCharsets.UTF_8))
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            cipher.doFinal(ciphertext)
        } catch (e: Exception) {
            e.printStackTrace()
            ByteArray(0)
        }
    }

    /**
     * AES解密(结果为16进制字符串)
     *
     * @param ciphertext 明文
     * @param key       密钥
     * @param iv        偏移量
     * @return 密文
     */
    @JvmStatic
    fun decryptAes(ciphertext: String, key: String, iv: String): String {
        return try {
            if (TextUtils.isEmpty(ciphertext)) throw IllegalArgumentException("The ciphertext cannot be empty.")
            val bytes = decryptAes(hexToByte(ciphertext), key, iv)
            return if (bytes.isEmpty()) "" else String(bytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * 检查AES加密key和iv是否合规
     * @param key AES 支持 128、192、256 位，也就是 16 / 24 / 32 字节
     * @param iv 固定要求 16 字节（因为 AES 的 block size = 128 位）
     */
    private fun checkAesKeyAndIv(key: String, iv: String) {
        if (TextUtils.isEmpty(key)) throw IllegalArgumentException("The key cannot be empty.")
        if (!(key.length == 16 || key.length == 24 || key.length == 32)) throw IllegalArgumentException("The key length must be 16/24/32 bytes.")
        if (TextUtils.isEmpty(iv)) throw IllegalArgumentException("The iv cannot be empty.")
        if (iv.length != 16) throw IllegalArgumentException("The iv length must be 16 bytes.")
    }

    /********************************** AES **********************************/

    /********************************** RSA **********************************/
    /**
     * 创建-RSA 密钥 一对儿 公私钥
     * 公钥加密-私钥解密：任何人都可以写，但只有自己能读（私钥持有者），称为“加密”。由于私钥是不公开的，确保了内容的保密，没有私钥无法获得内容
     * 私钥加密-公钥解密：任何人都可以读，但只有自己能写（私钥持有者），称为“签名”。由于公钥是公开的，任何人都可以解密内容，但只能用发布者的公钥解密，验证了内容是该发布者发出的
     *
     * @param keySize 密钥长度  一般都是2048  低了不安全   高了加密太慢
     * @return 公钥和私钥
     */
    @JvmStatic
    fun createRsaKey(keySize: Int = 2048): Map<String, String> {
        // 实例化密钥对生成器
        val generator = KeyPairGenerator.getInstance("RSA")
        // 设置密钥对长度和偏移量
        generator.initialize(RSAKeyGenParameterSpec(keySize, RSAKeyGenParameterSpec.F4))
        // 生成密钥对
        val keyPair: KeyPair = generator.generateKeyPair()
        // 创建使用私钥
        val privateKey: PrivateKey = keyPair.private
        // 创建使用公钥
        val publicKey: PublicKey = keyPair.public
        return mapOf(
            "publicKey" to encodeBase64Byte(publicKey.encoded),
            "privateKey" to encodeBase64Byte(privateKey.encoded)
        )
    }

    /** 公钥加密 */
    @JvmStatic
    fun encryptRsaPublic(plaintext: String, keyPublic: String): String {
        val keySpec = X509EncodedKeySpec(decodeBase64Byte(keyPublic))
        val keyFactory = KeyFactory.getInstance("RSA")
        val publicKey: PublicKey = keyFactory.generatePublic(keySpec)
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return encodeBase64Byte(cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8)))
    }

    /** 私钥加密（签名用） */
    @JvmStatic
    fun encryptRsaPrivate(plaintext: String, keyPrivate: String): String {
        val keySpec = PKCS8EncodedKeySpec(decodeBase64Byte(keyPrivate))
        val keyFactory = KeyFactory.getInstance("RSA")
        val privateKey: PrivateKey = keyFactory.generatePrivate(keySpec)
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, privateKey)
        return encodeBase64Byte(cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8)))
    }

    /** 公钥解密（解密私钥加密的） */
    @JvmStatic
    fun decryptRsaPublic(ciphertext: String, keyPublic: String): String {
        val keySpec = X509EncodedKeySpec(decodeBase64Byte(keyPublic))
        val keyFactory = KeyFactory.getInstance("RSA")
        val publicKey: PublicKey = keyFactory.generatePublic(keySpec)
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.DECRYPT_MODE, publicKey)
        return String(cipher.doFinal(decodeBase64Byte(ciphertext)), StandardCharsets.UTF_8)
    }

    /** 私钥解密（解密公钥加密的） */
    @JvmStatic
    fun decryptRsaPrivate(ciphertext: String, keyPrivate: String): String {
        val keySpec = PKCS8EncodedKeySpec(decodeBase64Byte(keyPrivate))
        val keyFactory = KeyFactory.getInstance("RSA")
        val privateKey: PrivateKey = keyFactory.generatePrivate(keySpec)
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        return String(cipher.doFinal(decodeBase64Byte(ciphertext)), StandardCharsets.UTF_8)
    }

    /********************************** RSA **********************************/

    @JvmStatic
    fun encodeBase64(plaintext: String): String {
        return Base64.encodeToString(plaintext.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
    }

    @JvmStatic
    fun encodeBase64Byte(plaintext: ByteArray): String {
        return Base64.encodeToString(plaintext, Base64.NO_WRAP)
    }

    @JvmStatic
    fun decodeBase64(ciphertext: String): String {
        return String(Base64.decode(ciphertext, Base64.NO_WRAP), StandardCharsets.UTF_8)
    }

    @JvmStatic
    fun decodeBase64Byte(ciphertext: String): ByteArray {
        return Base64.decode(ciphertext.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * 字节数组转成16进制字符串
     *
     * @param bytes 字节数组
     * @return 16进制字符串
     */
    @JvmStatic
    fun byteToHex(bytes: ByteArray): String {
        // 一个字节的数
        val sb = StringBuilder(bytes.size * 2)
        var temp: String
        for (byte in bytes) {
            // 整数转成十六进制表示
            temp = Integer.toHexString(byte.toInt() and 0XFF)
            if (temp.length == 1) {
                sb.append("0")
            }
            sb.append(temp)
        }
        // 转成大写
        return sb.toString().uppercase(Locale.getDefault())
    }

    /**
     * 将16进制字符串转换成字节数组
     *
     * @param hex 16进制字符串
     * @return 字节数组
     */
    @JvmStatic
    fun hexToByte(hex: String): ByteArray {
        // 如果字符串长度是奇数，就无法正确转换成字节数组,因为 1 个字节需要 2 个十六进制字符表示
        if (TextUtils.isEmpty(hex) || hex.length < 2 || hex.length % 2 == 0) return ByteArray(0)
        val tempHexStr = hex.lowercase()
        val len = tempHexStr.length / 2
        val result = ByteArray(len)
        for (i in 0 until len) {
            val temp = tempHexStr.substring(2 * i, 2 * i + 2)
            result[i] = (temp.toInt(16) and 0xFF).toByte()
        }
        return result
    }

    /** 16进制转10进制 */
    @JvmStatic
    fun hexToDec(hexString: String): BigInteger {
        require(hexString.matches(Regex("[0-9a-fA-F]+"))) { "Invalid hex string" }
        return BigInteger(hexString, 16)
    }

    /** 10进制转16进制 */
    @JvmStatic
    fun decToHex(decString: String): String {
        require(decString.matches(Regex("\\d+"))) { "Invalid decimal string" }
        return BigInteger(decString).toString(16).uppercase()
    }

    /**
     * 16进制异或加密
     *
     * @param msg 原文（十六进制字符串）
     * @return 密文
     */
    @JvmStatic
    fun encryptHexXor(msg: String): String {
        /* 如果字符串不是16进制字符串则抛出异常 */
        if (msg.isEmpty() || !msg.matches(Regex("[0-9a-fA-F]+"))) {
            throw IllegalArgumentException("Please pass in a hexadecimal string")
        }
        /* 利用随机数生成一个加密的16进制字符 */
        val randomValue = Random().nextInt(16) // 0 ~ 15
        val randomHexChar = if (randomValue < 10) {
            ('0'.code + randomValue).toChar()
        } else {
            ('A'.code + (randomValue - 10)).toChar()
        }
        // 使用Java 11+ 的 String.repeat 方法生成一个匹配原文长度的加密密钥，如原文为ABCD，加密字符为F，则加密密钥为FFFF
        // 目前暂定为统一加密字符，后续可根据需求为每一位字符配备加密字符，如1234
        val key = randomHexChar.toString().repeat(msg.length)
        /* 进行加密 */
        val encrypted = buildString {
            for (i in msg.indices) {
                /* 按照顺序依次取出原文字符和加密字符, 将各个字符转换成10进制整数值 */
                val msgByte = Character.digit(msg[i], 16)
                val keyByte = Character.digit(key[i], 16)
                // 进行异或操作
                val encryptedByte = msgByte xor keyByte
                // 使用String.format来确保始终为两位十六进制数
                append(String.format("%01X", encryptedByte))
            }
            // 将加密密钥添加至最后一个字符，后续可根据需求更换
            append(randomHexChar)
        }
        return encrypted
    }

    /**
     * 16进制异或解密
     *
     * @param encryptedMsg 密文
     * @return 原文
     */
    @JvmStatic
    fun decryptHexXor(encryptedMsg: String): String {
        /* 如果字符串不是16进制字符串则抛出异常 */
        if (encryptedMsg.isEmpty() || !encryptedMsg.matches(Regex("[0-9a-fA-F]+"))) {
            throw IllegalArgumentException("Please pass in a hexadecimal string")
        }
        // 提取加密字符 -- 因我们在加密算法中将加密字符放置于密文最后一位
        val keyChar = encryptedMsg.last()
        // 去掉最后的密钥字符，得到加密部分的密文
        val dataHex = encryptedMsg.dropLast(1)
        /* 根据密文长度生成密钥 */
        val key = keyChar.toString().repeat(dataHex.length)
        /* 进行解密 */
        val decrypted = buildString {
            for (i in dataHex.indices) {
                /* 按照顺序依次取出原文字符和加密字符, 将各个字符转换成10进制整数值 */
                val dataByte = Character.digit(dataHex[i], 16)
                val keyByte = Character.digit(key[i], 16)
                // 进行异或操作
                val decryptedByte = dataByte xor keyByte
                // 将整数转换回十六进制字符并添加到结果字符串中
                append(String.format("%01X", decryptedByte))
            }
        }
        return decrypted
    }

}