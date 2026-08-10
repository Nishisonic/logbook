package logbook.util;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.annotation.CheckForNull;

import logbook.internal.LoggerHolder;

import org.apache.commons.io.FilenameUtils;

/**
 * JavaBeanのutilです
 *
 */
public final class BeanUtils {

    private static final int MIN_XML_ENTITY_SIZE_LIMIT = 5_000_000;

    static {
        // JDK 24以降の既定値(100,000)では、XMLEncoderがエスケープした大きな文字列を含む
        // master.xmlを復元できないため、ローカル設定ファイルに必要な範囲まで引き上げる
        ensureXmlLimit("jdk.xml.maxGeneralEntitySizeLimit");
        ensureXmlLimit("jdk.xml.totalEntitySizeLimit");
    }

    /** ロガー */
    private static final LoggerHolder LOG = new LoggerHolder(BeanUtils.class);

    private static void ensureXmlLimit(String name) {
        String configured = System.getProperty(name);
        if (configured != null) {
            try {
                int value = Integer.parseInt(configured);
                if (value <= 0 || value >= MIN_XML_ENTITY_SIZE_LIMIT) {
                    return;
                }
            } catch (NumberFormatException e) {
                // 不正な設定値は、このアプリケーションで必要な値に置き換える
            }
        }
        System.setProperty(name, Integer.toString(MIN_XML_ENTITY_SIZE_LIMIT));
    }

    /**
     * JavaBeanオブジェクトをXML形式でファイルに書き込みます
     * 
     * @param file ファイル
     * @param obj JavaBean
     * @throws IOException IOException
     */
    public static void writeObject(File file, Object obj) throws IOException {
        File main = new File(FilenameUtils.removeExtension(file.getAbsolutePath()) + ".zip");
        if (main.exists()) {
            if (main.isDirectory()) {
                throw new IOException("File '" + main + "' exists but is a directory");
            }
            if (!(main.canWrite()))
                throw new IOException("File '" + main + "' cannot be written to");
        } else {
            File parent = main.getParentFile();
            if ((parent != null) &&
                    (!(parent.mkdirs())) && (!(parent.isDirectory()))) {
                throw new IOException("Directory '" + parent + "' could not be created");
            }
        }
        File backup = new File(FilenameUtils.removeExtension(file.getAbsolutePath()) + ".backup.zip");
        if ((main.exists() && (main.length() > 0)) && (!backup.exists() || backup.delete())) {
            // ファイルが存在してかつサイズが0を超える場合、バックアップを削除した後、ファイルをバックアップにリネームする
            main.renameTo(backup);
        }
        try (ZipOutputStream zos = new ZipOutputStream(
                new BufferedOutputStream(new FileOutputStream(main))))
        {
            ZipEntry zipentry = new ZipEntry(file.getName());
            zos.putNextEntry(zipentry);
            XMLEncoder oos = new XMLEncoder(zos);
            oos.writeObject(obj);
            oos.close();
        }
    }

    /**
     * <p>
     * XML形式で書き込まれたファイルからJavaBeanオブジェクトを復元します<br>
     * 復元時に型の検査を行います
     * </p>
     * 
     * @param file ファイル
     * @param clazz 期待する型
     * @return オブジェクト
     */
    @CheckForNull
    @SuppressWarnings("unchecked")
    public static <T> T readObject(File file, Class<T> clazz) {
        File target = new File(FilenameUtils.removeExtension(file.getAbsolutePath()) + ".zip");

        if (!target.canRead() || (target.length() <= 0)) {
            // ファイルが読み込めないまたはサイズがゼロの場合バックアップファイルを読み込む
            LOG.get().warn("次のファイルをバックアップから読み込みます: " + file.getName());
            target = new File(FilenameUtils.removeExtension(file.getAbsolutePath()) + ".backup.zip");
            if (!target.canRead()) {
                LOG.get().warn("バックアップも読み込めないので旧形式ファイルを読み込みます: " + file.getName());
                target = file;
                if (!target.canRead()) {
                    // バックアップファイルも読めない場合nullを返す
                    LOG.get().warn("旧形式ファイルも読み込めなかったので諦めます: " + file.getName());
                    return null;
                }
                try (XMLDecoder decoder = new XMLDecoder(new FileInputStream(target))) {
                    Object obj = decoder.readObject();
                    if (clazz.isInstance(obj)) {
                        return (T) obj;
                    }
                    return null;
                } catch (Exception e) {
                    return null;
                }
            }
        }
        try (ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(target))))
        {
            zis.getNextEntry();
            Object obj = (new XMLDecoder(zis).readObject());
            if (clazz.isInstance(obj)) {
                return (T) obj;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
