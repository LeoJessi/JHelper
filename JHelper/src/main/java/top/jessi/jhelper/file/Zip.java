package top.jessi.jhelper.file;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Created by Jessi on 2023/4/1 16:13
 * Email：17324719944@189.cn
 * Describe：文件压缩解压缩
 */
public class Zip {
    private static final int BUFFER_SIZE = 4096;

    /**
     * 压缩文件或文件夹
     *
     * @param srcFile  待压缩的文件或文件夹
     * @param destFile 压缩后的文件
     */
    public static void zip(File srcFile, File destFile) {
        ZipOutputStream out = null;
        try {
            out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(destFile)));
            zip(srcFile, out, "");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Files.closeQuietly(out);
        }
    }

    /**
     * 解压缩文件
     *
     * @param zipFile 待解压的压缩文件
     * @param destDir 解压后的文件夹
     */
    public static void unzip(File zipFile, File destDir) {
        ZipInputStream in = null;
        try {
            in = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                String path = destDir.getAbsolutePath() + "/" + entry.getName();
                if (entry.isDirectory()) {
                    new File(path).mkdirs();
                } else {
                    FileOutputStream out = null;
                    BufferedOutputStream bos = null;
                    byte[] buffer = new byte[BUFFER_SIZE];
                    try {
                        out = new FileOutputStream(path);
                        bos = new BufferedOutputStream(out, BUFFER_SIZE);
                        int count;
                        while ((count = in.read(buffer, 0, BUFFER_SIZE)) != -1) {
                            bos.write(buffer, 0, count);
                        }
                        bos.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        Files.closeQuietly(bos);
                        Files.closeQuietly(out);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Files.closeQuietly(in);
        }
    }

    private static void zip(File srcFile, ZipOutputStream out, String path) {
        if (srcFile.isDirectory()) {
            File[] files = srcFile.listFiles();
            if (files!= null) {
                for (File file : files) {
                    zip(file, out, path + srcFile.getName() + "/");
                }
            }
        } else {
            byte[] buffer = new byte[BUFFER_SIZE];
            FileInputStream in = null;
            BufferedInputStream bis = null;
            try {
                in = new FileInputStream(srcFile);
                bis = new BufferedInputStream(in, BUFFER_SIZE);
                out.putNextEntry(new ZipEntry(path + srcFile.getName()));
                int count;
                while ((count = bis.read(buffer, 0, BUFFER_SIZE)) != -1) {
                    out.write(buffer, 0, count);
                }
                out.closeEntry();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                Files.closeQuietly(bis);
                Files.closeQuietly(in);
            }
        }
    }

}
