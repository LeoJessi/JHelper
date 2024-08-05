package top.jessi.jhelper.file;

import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;

/**
 * Created by Jessi on 2023/4/1 15:08
 * Email：17324719944@189.cn
 * Describe：操作文件工具类
 */
public class Files {
    /**
     * 判断文件是否存在
     *
     * @param path 文件路径
     * @return true表示文件存在，false表示文件不存在
     */
    public static boolean isExists(String path) {
        return !TextUtils.isEmpty(path) && new File(path).exists();
    }

    /**
     * 获取文件类型
     *
     * @param file 文件
     * @return 文件类型
     */
    public static String getFileMimeType(File file) {
        if (file == null || !file.exists() || file.isDirectory()) {
            return "vnd.android.document/directory";
        }
        String mimeType = "";
        String extension = MimeTypeMap.getFileExtensionFromUrl(file.getAbsolutePath());
        if (TextUtils.isEmpty(extension)) {
            String fileName = file.getName();
            if (fileName.equals("") || fileName.endsWith(".")) {
                return "vnd.android.document/hidden";
            }
            int lastDotIndex = fileName.lastIndexOf(".");
            if (lastDotIndex != -1 && lastDotIndex < fileName.length() - 1) {
                extension = fileName.substring(lastDotIndex + 1).toLowerCase();
            }
        }
        if (!TextUtils.isEmpty(extension)) {
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        if (TextUtils.isEmpty(mimeType)) {
            mimeType = "application/octet-stream";
        }
        return mimeType;
    }

    /**
     * 获取文件大小
     *
     * @param path 文件路径
     * @return 文件大小（单位：字节）
     */
    public static long getSize(String path) {
        if (TextUtils.isEmpty(path)) {
            return 0;
        }
        File file = new File(path);
        if (!file.exists()) {
            return 0;
        }
        return file.length();
    }

    /**
     * 创建文件
     *
     * @param filePath 文件路径
     * @return true：创建成功 false：创建失败
     */
    public static boolean create(String filePath) {
        boolean isSuccess = false;
        File file = new File(filePath);
        if (!file.exists()) {
            try {
                /*根据文件路径截取出文件所在文件夹，如文件夹不存在则先创建文件夹*/
                String dirPath = filePath.substring(0, filePath.lastIndexOf("/"));
                if (!isExists(dirPath)) {
                    createDir(dirPath);
                }
                isSuccess = file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return isSuccess;
    }

    /**
     * 创建目录
     *
     * @param dirPath 目录路径
     * @return true：创建成功 false：创建失败
     */
    public static boolean createDir(String dirPath) {
        boolean isSuccess = false;
        File file = new File(dirPath);
        if (!file.exists()) {
            isSuccess = file.mkdirs();
        }
        return isSuccess;
    }

    /**
     * 复制文件
     *
     * @param sourceFilePath 资源文件路径
     * @param targetFilePath 目标文件路径
     * @return 是否复制成功
     */
    public static boolean copyFile(String sourceFilePath, String targetFilePath) {
        FileChannel sourceChannel = null;
        FileChannel targetChannel = null;
        boolean isSuccess = false;
        try {
            File sourceFile = new File(sourceFilePath);
            File targetFile = new File(targetFilePath);
            if (!sourceFile.exists()) return false;
            if (!isExists(targetFilePath)) if (!create(targetFilePath)) return false;
            sourceChannel = new FileInputStream(sourceFile).getChannel();
            targetChannel = new FileOutputStream(targetFile).getChannel();
            sourceChannel.transferTo(0, sourceChannel.size(), targetChannel);
            isSuccess = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeQuietly(sourceChannel);
            closeQuietly(targetChannel);
        }
        return isSuccess;
    }

    /**
     * 移动文件
     *
     * @param sourceFilePath 资源文件路径
     * @param targetFilePath 目标文件路径
     * @return 是否移动成功
     */
    public static boolean moveFile(String sourceFilePath, String targetFilePath) {
        try {
            File sourceFile = new File(sourceFilePath);
            File targetFile = new File(targetFilePath);
            if (!sourceFile.exists()) return false;
            if (!isExists(targetFilePath)) if (!create(targetFilePath)) return false;
            return sourceFile.renameTo(targetFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 删除文件或目录
     *
     * @param path 文件或目录的路径
     * @return true：删除成功 false：删除失败
     */
    public static boolean delete(String path) {
        boolean isSuccess = false;
        File file = new File(path);
        if (file.exists()) {
            if (!file.isFile()) {
                File[] files = file.listFiles();
                if (files != null) {
                    for (File f : files) {
                        delete(f.getAbsolutePath());
                    }
                }
            }
            isSuccess = file.delete();
        }
        return isSuccess;
    }

    /**
     * 读取文件内容
     *
     * @param filePath 文件路径
     * @return 文件内容
     */
    public static String read(String filePath) {
        FileInputStream inputStream = null;
        BufferedReader bufferedReader = null;
        StringBuilder stringBuilder = new StringBuilder();
        try {
            inputStream = new FileInputStream(filePath);
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(bufferedReader);
            closeQuietly(inputStream);
        }
        return stringBuilder.toString();
    }

    /**
     * 将字符串写入指定文件
     *
     * @param filePath 文件路径
     * @param content  内容
     * @param append   是否追加
     */
    public static boolean write(String filePath, String content, boolean append) {
        boolean isSuccess = false;
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(filePath, append);
            fileWriter.write(content);
            fileWriter.flush();
            isSuccess = true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(fileWriter);
        }
        return isSuccess;
    }

    /**
     * 关闭 Closeable 对象
     *
     * @param closeable Closeable 对象
     */
    public static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

