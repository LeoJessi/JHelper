package top.jessi.jhelper.enigma;

import android.text.TextUtils;
import android.util.Base64;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by Jessi on 2022/8/12 17:06
 * Email：17324719944@189.cn
 * Describe：加密解密算法
 */
public class Enigma {
    private Enigma() {
        /*私有化构造方法,阻止外部直接实例化对象*/
        throw new UnsupportedOperationException("cannot be instantiated");
    }

    /**
     * AES加密(结果为16进制字符串)
     *
     * @param msg 明文
     * @param key 密钥
     * @param iv  偏移量
     * @return 密文
     */
    public static String encryptAes(String msg, String key, String iv) {
        byte[] data = null;
        try {
            data = msg.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        data = encryptAes(data, key, iv);
        if (data != null) return byteToHex(data);
        return null;
    }

    /**
     * AES解密(输出结果为字符串)
     *
     * @param encryptedMsg 密文
     * @param key          密钥
     * @param iv           偏移量
     * @return 明文
     */
    public static String decryptAes(String encryptedMsg, String key, String iv) {
        byte[] data = null;
        try {
            data = hexToByte(encryptedMsg);
        } catch (Exception e) {
            e.printStackTrace();
        }
        data = decryptAes(data, key, iv);
        if (data == null) return null;
        String result;
        result = new String(data, StandardCharsets.UTF_8);
        return result;
    }

    private static byte[] encryptAes(byte[] content, String key, String iv) {
        try {
            SecretKeySpec aesKey = createAesKey(key);
            // AES算法/模式/填充
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, createAesIv(iv));
            return cipher.doFinal(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static byte[] decryptAes(byte[] content, String key, String iv) {
        try {
            SecretKeySpec aesKey = createAesKey(key);
            /* 算法/模式/填充 */
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, aesKey, createAesIv(iv));
            return cipher.doFinal(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * AES密钥 --- 不足16位时补零 超过16位截取
     *
     * @param key AES密钥
     * @return 处理后的AES密钥
     */
    private static SecretKeySpec createAesKey(String key) {
        byte[] data;
        if (key == null) key = "";
        StringBuilder sb = new StringBuilder(16);
        sb.append(key);
        while (sb.length() < 16) {
            sb.append("0");
        }
        if (sb.length() > 16) sb.setLength(16);
        data = sb.toString().getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(data, "AES");
    }

    /**
     * AES偏移量 --- 不足16位时补零 超过16位截取
     *
     * @param iv AES偏移量
     * @return 处理后的AES偏移量
     */
    private static IvParameterSpec createAesIv(String iv) {
        byte[] data;
        if (iv == null) iv = "";
        StringBuilder sb = new StringBuilder(16);
        sb.append(iv);
        while (sb.length() < 16) {
            sb.append("0");
        }
        if (sb.length() > 16) sb.setLength(16);
        data = sb.toString().getBytes(StandardCharsets.UTF_8);
        return new IvParameterSpec(data);
    }

    /**
     * 字节数组转成16进制字符串
     *
     * @param b 字节数组
     * @return 16进制字符串
     */
    private static String byteToHex(byte[] b) { // 一个字节的数，
        StringBuilder sb = new StringBuilder(b.length * 2);
        String tmp;
        for (byte value : b) {
            // 整数转成十六进制表示
            tmp = (Integer.toHexString(value & 0XFF));
            if (tmp.length() == 1) {
                sb.append("0");
            }
            sb.append(tmp);
        }
        return sb.toString().toUpperCase(); // 转成大写
    }

    /**
     * 将字符串转换成字节数组
     *
     * @param inputString 字符串
     * @return 字节数组
     */
    private static byte[] hexToByte(String inputString) {
        if (inputString == null || inputString.length() < 2) {
            return new byte[0];
        }
        inputString = inputString.toLowerCase();
        int l = inputString.length() / 2;
        byte[] result = new byte[l];
        for (int i = 0; i < l; ++i) {
            String tmp = inputString.substring(2 * i, 2 * i + 2);
            result[i] = (byte) (Integer.parseInt(tmp, 16) & 0xFF);
        }
        return result;
    }

    /**
     * MD5编码(不可逆)
     *
     * @param msg 明文
     * @return 密文
     */
    public static String md5(String msg) {
        byte[] hash;
        try {
            hash = MessageDigest.getInstance("MD5").digest(msg.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }
        StringBuilder hex = new StringBuilder(hash.length * 2);
        byte[] var3 = hash;
        int var4 = hash.length;
        for (int var5 = 0; var5 < var4; ++var5) {
            byte b = var3[var5];
            if ((b & 255) < 16) {
                hex.append("0");
            }
            hex.append(Integer.toHexString(b & 255));
        }
        return hex.toString();
    }

    /**
     * SHA1编码(不可逆)
     *
     * @param msg 明文
     * @return 密文
     */
    public static String sha1(String msg) {
        // 加密字符
        final char[] chars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        if (msg == null) return "";
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
            messageDigest.update(msg.getBytes());
            byte[] digest = messageDigest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(chars[(b >> 4) & 15]);
                sb.append(chars[b & 15]);
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * SHA256
     *
     * @param msg 明文
     * @return 密文
     */
    public static String sha256(String msg) {
        // 加密字符
        final char[] chars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        if (msg == null) return "";
        try {
            // 创建摘要算法对象
            MessageDigest messageDigest = MessageDigest.getInstance("SHA256");
            messageDigest.update(msg.getBytes());
            byte[] digest = messageDigest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(chars[(b >> 4) & 15]);
                sb.append(chars[b & 15]);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * SHA384
     *
     * @param msg 明文
     * @return 密文
     */
    public static String sha384(String msg) {
        // 加密字符
        final char[] chars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        if (msg == null) return "";
        try {
            // 创建摘要算法对象
            MessageDigest messageDigest = MessageDigest.getInstance("SHA384");
            messageDigest.update(msg.getBytes());
            byte[] digest = messageDigest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(chars[(b >> 4) & 15]);
                sb.append(chars[b & 15]);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * SHA512
     *
     * @param msg 明文
     * @return 密文
     */
    public static String sha512(String msg) {
        // 加密字符
        final char[] chars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        if (msg == null) return "";
        try {
            // 创建摘要算法对象
            MessageDigest messageDigest = MessageDigest.getInstance("SHA512");
            messageDigest.update(msg.getBytes());
            byte[] digest = messageDigest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(chars[(b >> 4) & 15]);
                sb.append(chars[b & 15]);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 使用凯撒加密方式加密数据
     *
     * @param msg 明文
     * @param key 密钥
     * @return 密文
     */
    public static String encryptCaesar(String msg, int key) {
        // 将字符串转换为数组
        char[] chars = msg.toCharArray();
        StringBuilder buffer = new StringBuilder();
        // 遍历数组
        for (char aChar : chars) {
            // 获取字符的ASCII编码
            int asciiCode = aChar;
            // 偏移数据
            asciiCode += key;
            // 将偏移后的数据转为字符
            char result = (char) asciiCode;
            // 拼接数据
            buffer.append(result);
        }
        return buffer.toString();
    }

    /**
     * 解密凯撒加密
     *
     * @param encryptedMsg 密文
     * @param key          密钥
     * @return 明文
     */
    public static String decryptCaesar(String encryptedMsg, int key) {
        // 将字符串转为字符数组
        char[] chars = encryptedMsg.toCharArray();
        StringBuilder sb = new StringBuilder();
        // 遍历数组
        for (char aChar : chars) {
            // 获取字符的ASCII编码
            int asciiCode = aChar;
            // 偏移数据
            asciiCode -= key;
            // 将偏移后的数据转为字符
            char result = (char) asciiCode;
            // 拼接数据
            sb.append(result);
        }
        return sb.toString();
    }

    /**
     * 维吉尼亚加密
     *
     * @param msg 明文
     * @param key 密钥
     * @return 密文
     */
    public static String encryptVirginia(String msg, String key) {
        StringBuilder result = new StringBuilder();
        for (int i = 0, j = 0; i < msg.length(); i++) {
            char c = msg.charAt(i);
            if (Character.isLetter(c)) {
                if (Character.isUpperCase(c)) {
                    result.append((char) ((c + key.toUpperCase().charAt(j) - 2 * 'A') % 26 + 'A'));
                } else {
                    result.append((char) ((c + key.toLowerCase().charAt(j) - 2 * 'a') % 26 + 'a'));
                }
            } else {
                result.append(c);
            }
            j = ++j % key.length();
        }
        return result.toString();
    }

    /**
     * 维吉尼亚解密
     *
     * @param encryptedMsg 密文
     * @param key          密钥
     * @return 明文
     */
    public static String decryptVirginia(String encryptedMsg, String key) {
        StringBuilder result = new StringBuilder();
        for (int i = 0, j = 0; i < encryptedMsg.length(); i++) {
            char c = encryptedMsg.charAt(i);
            if (Character.isLetter(c)) {
                if (Character.isUpperCase(c)) {
                    result.append((char) ('Z' - (25 - (c - key.toUpperCase().charAt(j))) % 26));
                } else {
                    result.append((char) ('z' - (25 - (c - key.toLowerCase().charAt(j))) % 26));
                }
            } else {
                result.append(c);
            }
            j = ++j % key.length();
        }
        return result.toString();
    }

    /**
     * base64编码
     *
     * @param msg 明文
     * @return 密文
     */
    public static String encodeBase64(String msg) {
        return Base64.encodeToString(msg.getBytes(), Base64.NO_WRAP).trim();
    }

    /**
     * base64解码
     *
     * @param encodedMsg 密文
     * @return 明文
     */
    public static String decodeBase64(String encodedMsg) {
        return new String(Base64.decode(encodedMsg, Base64.NO_WRAP)).trim();
    }

    private static String encodeBase64Byte(byte[] msg) {
        return Base64.encodeToString(msg, Base64.NO_WRAP).trim();
    }

    private static byte[] decodeBase64Byte(String msg) {
        return Base64.decode(msg.getBytes(), Base64.NO_WRAP);
    }

    /**
     * 创建-RSA 密钥 一对儿 公私钥
     * 公钥加密-私钥解密：任何人都可以写，但只有自己能读（私钥持有者），称为“加密”。由于私钥是不公开的，确保了内容的保密，没有私钥无法获得内容
     * 私钥加密-公钥解密：任何人都可以读，但只有自己能写（私钥持有者），称为“签名”。由于公钥是公开的，任何人都可以解密内容，但只能用发布者的公钥解密，验证了内容是该发布者发出的
     *
     * @param keySize 密钥长度  一般都是1024  低了不安全   高了加密太慢
     * @return 公钥和私钥
     */
    public static HashMap<String, String> createRsaKey(int keySize) {
        try {
            // 实例化密钥对生成器
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            // 设置密钥对长度和偏移量
            generator.initialize(new RSAKeyGenParameterSpec(keySize, RSAKeyGenParameterSpec.F4));
            // 生成密钥对
            KeyPair keyPair = generator.generateKeyPair();
            // 创建使用私钥
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
            // 创建使用公钥
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            // 封装密钥对
            HashMap<String, String> keys = new HashMap<>();
            keys.put("publicKey", encodeBase64Byte(publicKey.getEncoded()));
            keys.put("privateKey", encodeBase64Byte(privateKey.getEncoded()));
            return keys;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * RSA 公钥-加密
     *
     * @param msg       明文
     * @param keyPublic 公钥
     * @return 密文
     */
    public static String encryptRsaPublic(String msg, String keyPublic) {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodeBase64Byte(keyPublic));
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(keySpec);
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return encodeBase64Byte(cipher.doFinal(msg.getBytes()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * RSA  私钥-加密
     *
     * @param msg        明文
     * @param keyPrivate 私钥
     * @return 密文
     */
    public static String encryptRsaPrivate(String msg, String keyPrivate) {
        try {
            // 转换私钥
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodeBase64Byte(keyPrivate));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, privateKey);
            return encodeBase64Byte(cipher.doFinal(msg.getBytes()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * RSA  公钥-解密
     *
     * @param msgPrivate 私钥加密过的密文
     * @param keyPublic  公钥
     * @return 明文
     */
    public static String decryptRsaPublic(String msgPrivate, String keyPublic) {
        try {
            // 转换公钥
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodeBase64Byte(keyPublic));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(keySpec);
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, publicKey);
            return new String(cipher.doFinal(decodeBase64Byte(msgPrivate)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * RSA  私钥-解密
     *
     * @param msgPublic  公钥加密过的密文
     * @param keyPrivate 私钥
     * @return 明文
     */
    public static String decryptRsaPrivate(String msgPublic, String keyPrivate) {
        try {
            // 转换私钥
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodeBase64Byte(keyPrivate));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return new String(cipher.doFinal(decodeBase64Byte(msgPublic)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 栅栏加密
     *
     * @param msg 明文
     * @param key 密钥 --> 不可超过明文长度
     * @return 密文
     */
    public static String encryptFence(String msg, int key) {
        char[][] fence = new char[key][msg.length()];
        boolean down = false;
        int rail = 0;
        for (int i = 0; i < msg.length(); i++) {
            fence[rail][i] = msg.charAt(i);
            if (rail == 0 || rail == key - 1) {
                down = !down;
            }
            rail += down ? 1 : -1;
        }
        StringBuilder ciphertext = new StringBuilder();
        for (int i = 0; i < key; i++) {
            for (int j = 0; j < msg.length(); j++) {
                if (fence[i][j] != 0) {
                    ciphertext.append(fence[i][j]);
                }
            }
        }
        return ciphertext.toString();
    }

    /**
     * 栅栏解密
     *
     * @param encryptedMsg 密文
     * @param key          密钥 --> 不可超过密文长度
     * @return 明文
     */
    public static String decryptFence(String encryptedMsg, int key) {
        char[][] fence = new char[key][encryptedMsg.length()];
        boolean down = false;
        int rail = 0;
        for (int i = 0; i < encryptedMsg.length(); i++) {
            fence[rail][i] = '*';
            if (rail == 0 || rail == key - 1) {
                down = !down;
            }
            rail += down ? 1 : -1;
        }
        int index = 0;
        for (int i = 0; i < key; i++) {
            for (int j = 0; j < encryptedMsg.length(); j++) {
                if (fence[i][j] == '*' && index < encryptedMsg.length()) {
                    fence[i][j] = encryptedMsg.charAt(index++);
                }
            }
        }
        StringBuilder plaintext = new StringBuilder();
        down = false;
        rail = 0;
        for (int i = 0; i < encryptedMsg.length(); i++) {
            plaintext.append(fence[rail][i]);
            if (rail == 0 || rail == key - 1) {
                down = !down;
            }
            rail += down ? 1 : -1;
        }
        return plaintext.toString();
    }

    /**
     * 16进制异或加密
     *
     * @param msg 原文
     * @return 密文
     */
    public static String encryptHexXor(String msg) {
        /* 如果字符串不是16进制字符串则抛出异常 */
        if (TextUtils.isEmpty(msg) || !msg.matches("[0-9a-fA-F]+")) {
            throw new IllegalArgumentException("Please pass in a hexadecimal string");
        }
        /* 利用随机数生成一个加密的16进制字符 */
        Random random = new Random();
        char randomHexChar = (char) (random.nextInt(16) < 10 ? '0' + random.nextInt(10) : 'A' + random.nextInt(6));
        // 使用Java 11+ 的 String.repeat 方法生成一个匹配原文长度的加密密钥，如原文为ABCD，加密字符为F，则加密密钥为FFFF
        // 目前暂定为统一加密字符，后续可根据需求为每一位字符配备加密字符，如1234
        String key = String.valueOf(randomHexChar).repeat(msg.length());
        /* 进行加密 */
        StringBuilder encrypted = new StringBuilder();
        for (int i = 0, len = msg.length(); i < len; i++) {
            /* 按照顺序依次取出原文字符和加密字符 */
            char msgChar = msg.charAt(i);
            char keyChar = key.charAt(i);
            /* 将各个字符转换成10进制整数值 */
            int msgByte = Character.digit(msgChar, 16);
            int keyByte = Character.digit(keyChar, 16);
            // 进行异或操作
            int encryptedByte = msgByte ^ keyByte;
            // 使用String.format来确保始终为两位十六进制数
            encrypted.append(String.format("%01X", encryptedByte));
        }
        // 将加密密钥添加至最后一个字符，后续可根据需求更换
        encrypted.append(randomHexChar);
        return encrypted.toString();
    }

    /**
     * 16进制异或解密
     *
     * @param encryptedMsg 密文
     * @return 原文
     */
    public static String decryptHexXor(String encryptedMsg) {
        /* 如果字符串不是16进制字符串则抛出异常 */
        if (TextUtils.isEmpty(encryptedMsg) || !encryptedMsg.matches("[0-9a-fA-F]+")) {
            throw new IllegalArgumentException("Please pass in a hexadecimal string");
        }
        // 提取加密字符 -- 因我们在加密算法中将加密字符放置于密文最后一位
        char keyChar = encryptedMsg.charAt(encryptedMsg.length() - 1);
        // 去掉最后的密钥字符，得到加密部分的密文
        String dataHex = encryptedMsg.substring(0, encryptedMsg.length() - 1);
        /* 根据密文长度生成密钥 */
        StringBuilder key = new StringBuilder();
        for (int i = 0, len = dataHex.length(); i < len; i++) {
            key.append(keyChar);
        }
        /* 进行解密 */
        StringBuilder decrypted = new StringBuilder();
        for (int i = 0, len = dataHex.length(); i < len; i++) {
            /* 按照顺序依次取出原文字符和加密字符 */
            char dataChar = dataHex.charAt(i);
            char keyCharForDecrypt = key.charAt(i);
            /* 将各个字符转换成10进制整数值 */
            int dataByte = Character.digit(dataChar, 16);
            int keyByte = Character.digit(keyCharForDecrypt, 16);
            // 进行异或操作
            int decryptedByte = dataByte ^ keyByte;
            // 将整数转换回十六进制字符并添加到结果字符串中
            decrypted.append(String.format("%01X", decryptedByte));
        }
        return decrypted.toString();
    }

    /**
     * 16进制转10进制
     *
     * @param hexString 16进制字符串
     * @return 10进制字符串
     */
    public static BigInteger hexToDec(String hexString) {
        BigInteger dec = BigInteger.ZERO;
        int length = hexString.length();
        for (int i = 0; i < length; i++) {
            char currentValue = hexString.charAt(i);
            // 将十六进制字符转换为十进制数字
            int digit = Character.digit(currentValue, 16);
            dec = dec.multiply(BigInteger.valueOf(16)).add(BigInteger.valueOf(digit));
        }
        return dec;
    }

    /**
     * 10进制转16进制
     *
     * @param decString 10进制字符串
     * @return 16进制字符串
     */
    public static String decToHex(String decString) {
        // 先将10进制字符串传入BigInteger的构造函数，否则遇到数字太大会报错
        BigInteger bigInteger = new BigInteger(decString);
        return bigInteger.toString(16).toUpperCase();
    }

}
