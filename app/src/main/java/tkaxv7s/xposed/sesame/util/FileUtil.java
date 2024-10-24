package tkaxv7s.xposed.sesame.util;

import android.os.Environment;
import java.io.*;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import tkaxv7s.xposed.sesame.hook.Toast;

public class FileUtil {
  private static final String TAG = FileUtil.class.getSimpleName();

  public static final String CONFIG_DIRECTORY_NAME = "sesame";
  public static final File MAIN_DIRECTORY_FILE = getMainDirectoryFile();
  public static final File CONFIG_DIRECTORY_FILE = getConfigDirectoryFile();
  public static final File LOG_DIRECTORY_FILE = getLogDirectoryFile();
  private static File cityCodeFile;
  private static File wuaFile;

  /**
   * 确保指定的目录存在，如果存在且为文件则删除它并创建目录。
   *
   * @param dir 要确保存在的目录。
   * @return 确保存在的目录File对象。
   */
  private static File ensureDirectory(File dir) {
    if (dir.exists()) {
      if (dir.isFile()) {
        dir.delete(); // 删除文件
        dir.mkdirs(); // 创建目录
      }
    } else {
      dir.mkdirs(); // 创建目录
    }
    return dir;
  }

  /**
   * 获取主目录文件。
   *
   * @return 主目录File对象。
   */
  private static File getMainDirectoryFile() {
    String storageDirStr = Environment.getExternalStorageDirectory() + File.separator + "Android" + File.separator + "media" + File.separator + ClassUtil.PACKAGE_NAME;
    File storageDir = new File(storageDirStr);
    File mainDir = new File(storageDir, CONFIG_DIRECTORY_NAME);
    return ensureDirectory(mainDir);
  }

  /**
   * 获取日志目录文件。
   *
   * @return 日志目录File对象。
   */
  private static File getLogDirectoryFile() {
    File logDir = new File(MAIN_DIRECTORY_FILE, "log");
    return ensureDirectory(logDir);
  }

  /**
   * 获取配置目录文件。
   *
   * @return 配置目录File对象。
   */
  private static File getConfigDirectoryFile() {
    File configDir = new File(MAIN_DIRECTORY_FILE, "config");
    return ensureDirectory(configDir);
  }

  /**
   * 获取用户配置目录文件。
   *
   * @param userId 用户ID
   * @return 用户配置目录File对象。
   */
  public static File getUserConfigDirectoryFile(String userId) {
    File configDir = new File(CONFIG_DIRECTORY_FILE, userId);
    return ensureDirectory(configDir);
  }

  public static File getDefaultConfigV2File() {
    return new File(MAIN_DIRECTORY_FILE, "config_v2.json");
  }

  public static boolean setDefaultConfigV2File(String json) {
    return write2File(json, new File(MAIN_DIRECTORY_FILE, "config_v2.json"));
  }

  public static File getConfigV2File(String userId) {
    File file = new File(CONFIG_DIRECTORY_FILE + "/" + userId, "config_v2.json");
    if (!file.exists()) {
      File oldFile = new File(CONFIG_DIRECTORY_FILE, "config_v2-" + userId + ".json");
      if (oldFile.exists()) {
        if (write2File(readFromFile(oldFile), file)) {
          oldFile.delete();
        } else {
          file = oldFile;
        }
      }
    }
    return file;
  }

  public static boolean setConfigV2File(String userId, String json) {
    return write2File(json, new File(CONFIG_DIRECTORY_FILE + "/" + userId, "config_v2.json"));
  }

  public static boolean setUIConfigFile(String json) {
    return write2File(json, new File(MAIN_DIRECTORY_FILE, "ui_config.json"));
  }

  public static File getSelfIdFile(String userId) {
    File file = new File(CONFIG_DIRECTORY_FILE + "/" + userId, "self.json");
    ensureFile(file);
    return file;
  }

  public static File getFriendIdMapFile(String userId) {
    File file = new File(CONFIG_DIRECTORY_FILE + "/" + userId, "friend.json");
    ensureFile(file);
    return file;
  }

  public static File runtimeInfoFile(String userId) {
    File runtimeInfoFile = new File(CONFIG_DIRECTORY_FILE + "/" + userId, "runtimeInfo.json");
    if (!runtimeInfoFile.exists()) {
      try {
        runtimeInfoFile.createNewFile();
      } catch (Throwable ignored) {
      }
    }
    return runtimeInfoFile;
  }

  public static File getCooperationIdMapFile(String userId) {
    File file = new File(CONFIG_DIRECTORY_FILE + "/" + userId, "cooperation.json");
    ensureFile(file);
    return file;
  }

  public static File getStatusFile(String userId) {
    File file = new File(CONFIG_DIRECTORY_FILE + "/" + userId, "status.json");
    ensureFile(file);
    return file;
  }

  public static File getStatisticsFile() {
    File statisticsFile = new File(MAIN_DIRECTORY_FILE, "statistics.json");
    ensureFile(statisticsFile);
    if (statisticsFile.exists()) {
      Log.i(TAG, "[statistics]读:" + statisticsFile.canRead() + ";写:" + statisticsFile.canWrite());
    } else {
      Log.i(TAG, "statisticsFile.json文件不存在");
    }
    return statisticsFile;
  }

  public static File getReserveIdMapFile() {
    File file = new File(MAIN_DIRECTORY_FILE, "reserve.json");
    ensureFile(file);
    return file;
  }

  public static File getBeachIdMapFile() {
    File file = new File(MAIN_DIRECTORY_FILE, "beach.json");
    ensureFile(file);
    return file;
  }

  public static File getUIConfigFile() {
    File file = new File(MAIN_DIRECTORY_FILE, "ui_config.json");
    ensureFile(file);
    return file;
  }

  public static File getWuaFile() {
    if (wuaFile == null) {
      wuaFile = new File(MAIN_DIRECTORY_FILE, "wua.list");
      ensureFile(wuaFile);
    }
    return wuaFile;
  }

  public static File getCityCodeFile() {
    if (cityCodeFile == null) {
      cityCodeFile = new File(MAIN_DIRECTORY_FILE, "cityCode.json");
      ensureFile(cityCodeFile);
    }
    return cityCodeFile;
  }

  public static File getExportedStatisticsFile() {
    String storageDirStr = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + CONFIG_DIRECTORY_NAME;
    File storageDir = new File(storageDirStr);
    if (!storageDir.exists()) {
      storageDir.mkdirs();
    }
    File exportedStatisticsFile = new File(storageDir, "statistics.json");
    if (exportedStatisticsFile.exists() && exportedStatisticsFile.isDirectory()) {
      exportedStatisticsFile.delete();
    }
    return exportedStatisticsFile;
  }

  public static File getFriendWatchFile() {
    File friendWatchFile = new File(MAIN_DIRECTORY_FILE, "friendWatch.json");
    if (friendWatchFile.exists() && friendWatchFile.isDirectory()) {
      friendWatchFile.delete();
    }
    return friendWatchFile;
  }

  public static File exportFile(File file) {
    String exportDirStr = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + CONFIG_DIRECTORY_NAME;
    File exportDir = new File(exportDirStr);
    if (!exportDir.exists()) {
      exportDir.mkdirs();
    }
    File exportFile = new File(exportDir, file.getName());
    if (exportFile.exists() && exportFile.isDirectory()) {
      exportFile.delete();
    }
    if (FileUtil.copyTo(file, exportFile)) {
      return exportFile;
    }
    return null;
  }

  /**
   * 确保指定的文件存在，如果不存在则创建它。 如果文件已存在且为目录，则会递归删除该目录及其所有内容。
   *
   * @param file 要确保存在的文件。
   */
  public static void ensureFile(File file) {
    // 如果文件已存在且为目录，则尝试删除它
    if (file.exists() && file.isDirectory()) {
      deleteDirectory(file);
    }
    // 如果文件不存在，则尝试创建它
    if (!file.exists()) {
      createFile(file);
    }
  }

  /**
   * 获取指定类型的日志文件。
   *
   * @param logFileName 日志文件的名称。
   * @return 对应类型的日志文件的File对象。
   */
  public static File getLogFile(String logFileName) {
    File logFile = new File(LOG_DIRECTORY_FILE, Log.getLogFileName(logFileName));
    ensureFile(logFile);
    return logFile;
  }

  public static File getRuntimeLogFile() {
    return getLogFile("runtime");
  }

  public static File getRecordLogFile() {
    return getLogFile("record");
  }

  public static File getSystemLogFile() {
    return getLogFile("system");
  }

  public static File getDebugLogFile() {
    return getLogFile("debug");
  }

  public static File getForestLogFile() {
    return getLogFile("forest");
  }

  /**
   * 获取农场日志文件。 如果日志文件已存在且为目录，则删除它。 如果日志文件不存在，则创建一个新的文件。
   *
   * @return 农场日志文件的File对象。
   */
  public static File getFarmLogFile() {
    return getLogFile("farm");
  }

  /**
   * 获取其他类型的日志文件。 如果日志文件已存在且为目录，则删除它。 如果日志文件不存在，则创建一个新的文件。
   *
   * @return 其他类型的日志文件的File对象。
   */
  public static File getOtherLogFile() {
    return getLogFile("other");
  }

  /**
   * 获取错误日志文件。 如果错误日志文件已存在且为目录，则删除它。 如果错误日志文件不存在，则创建一个新的文件。
   *
   * @return 错误日志文件的File对象。
   */
  public static File getErrorLogFile() {
    return getLogFile("error");
  }

  /**
   * 递归删除目录及其所有内容。
   *
   * @param directory 要删除的目录。
   */
  private static void deleteDirectory(File directory) {
    if (directory.isDirectory()) {
      File[] files = directory.listFiles();
      if (files != null) {
        for (File file : files) {
          deleteDirectory(file);
        }
      }
    }
    // 尝试删除文件或目录
    try {
      boolean deleted = directory.delete();
      if (!deleted) {
        throw new IOException("无法删除文件或目录: " + directory.getAbsolutePath());
      }
    } catch (IOException e) {
      Log.printStackTrace(e);
    }
  }

  /** 清除日志文件。 此方法会删除日志目录中所有非当天的日志文件，以及超过100MB的当天日志文件。 */
  public static void clearLog() {
    // 获取日志目录中的所有文件
    File[] files = LOG_DIRECTORY_FILE.listFiles();
    if (files == null) {
      // 如果文件数组为空，直接返回
      return;
    }
    // 获取当前日期的格式化对象
    SimpleDateFormat sdf = Log.DATE_FORMAT_THREAD_LOCAL.get();
    if (sdf == null) {
      // 如果格式化对象为空，则新建一个默认的格式化对象
      sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    }
    // 格式化当前日期
    String today = sdf.format(new Date());
    // 遍历日志目录中的所有文件
    for (File file : files) {
      String name = file.getName();
      // 检查文件名是否以当天日期加.log结尾
      if (name.endsWith(today + ".log")) {
        // 如果文件大小小于100MB，则跳过不删除
        if (file.length() < 104_857_600) {
          continue;
        }
      }
      // 尝试删除文件
      try {
        file.delete();
      } catch (Exception e) {
        // 如果删除失败，记录异常信息
        Log.printStackTrace(e);
      }
    }
  }

  /**
   * 从文件中读取内容到字符串。
   *
   * @param file 要读取的文件。
   * @return 文件内容的字符串表示，如果文件不存在或无法读取，则返回空字符串。
   */
  public static String readFromFile(File file) {
    // 检查文件是否存在
    if (!file.exists()) {
      // 如果文件不存在，则返回空字符串
      return "";
    }
    // 检查文件是否可读
    if (!file.canRead()) {
      // 如果文件不可读，则显示提示信息并返回空字符串
      Toast.show(file.getName() + "没有读取权限！", true);
      return "";
    }

    // 使用StringBuilder来累积读取的文件内容
    StringBuilder content = new StringBuilder();
    try (FileReader fr = new FileReader(file)) {
      // 创建字符缓冲区
      char[] buffer = new char[1024];
      // 读取文件内容到缓冲区
      int length;
      // 继续读取直到文件末尾
      while ((length = fr.read(buffer)) != -1) {
        // 将缓冲区内容追加到StringBuilder
        content.append(buffer, 0, length);
      }
    } catch (IOException e) {
      // 记录异常信息
      Log.printStackTrace(TAG, e);
    }
    // 返回文件内容的字符串表示
    return content.toString();
  }

  public static boolean write2File(String s, File f) {
    if (f.exists()) {
      if (!f.canWrite()) {
        Toast.show(f.getAbsoluteFile() + "没有写入权限！", true);
        return false;
      }
      if (f.isDirectory()) {
        f.delete();
        Objects.requireNonNull(f.getParentFile()).mkdirs();
      }
    } else {
      Objects.requireNonNull(f.getParentFile()).mkdirs();
    }
    boolean success = false;
    FileWriter fw = null;
    try {
      fw = new FileWriter(f);
      fw.write(s);
      fw.flush();
      success = true;
    } catch (Throwable t) {
      Log.printStackTrace(TAG, t);
    }
    close(fw);
    return success;
  }

  /**
   * 向文件追加字符串内容。 如果文件不存在，将会创建该文件；如果文件不可写，将弹出提示。
   *
   * @param s 要追加的字符串内容
   * @param f 目标文件
   * @return 如果字符串成功追加到文件中，则返回 true；否则返回 false
   */
  public static boolean append2File(String s, File f) {
    // 检查文件是否存在且可写
    if (f.exists() && !f.canWrite()) {
      Toast.show(f.getAbsolutePath() + "没有写入权限！", true);
      return false;
    }

    try (FileWriter fw = new FileWriter(f, true)) {
      fw.append(s);
      fw.flush();
      return true;
    } catch (IOException e) {
      // 打印异常堆栈跟踪
      Log.printStackTrace(e);
      return false;
    }
  }

  /**
   * 将文件从源路径复制到目标路径。 使用 NIO 的 FileChannel 进行高效的文件复制。
   *
   * @param source 源文件，不能为 null
   * @param dest 目标文件，不能为 null
   * @return 如果文件复制成功，则返回 true；否则返回 false
   */
  public static boolean copyTo(File source, File dest) {
    if (source == null || dest == null || !source.exists() || !source.isFile()) {
      // 如果源文件不存在或不是文件，则直接返回 false
      return false;
    }

    try (FileInputStream fis = new FileInputStream(source);
        FileChannel inputChannel = fis.getChannel()) {
      // 使用 createFile 方法确保目标文件存在
      if (!Objects.requireNonNull(createFile(dest)).exists()) {
        return false;
      }

      try (FileOutputStream fos = new FileOutputStream(dest);
          FileChannel outputChannel = fos.getChannel()) {
        // 使用 transferFrom 方法从输入通道复制到输出通道
        outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
        return true;
      } catch (IOException e) {
        // 打印异常堆栈跟踪
        Log.printStackTrace(e);
        return false;
      }
    } catch (IOException e) {
      // 打印异常堆栈跟踪
      Log.printStackTrace(e);
      return false;
    }
  }

  /**
   * 将数据从 InputStream 流复制到 OutputStream 流。 此方法会捕获并记录在流操作过程中可能发生的任何异常。
   *
   * @param source 源 InputStream 对象，不能为 null
   * @param dest 目标 OutputStream 对象，不能为 null
   * @return 如果数据成功复制，则返回 true；否则返回 false
   */
  public static boolean streamTo(InputStream source, OutputStream dest) {
    if (source == null || dest == null) {
      return false;
    }

    try {
      byte[] buffer = new byte[1024];
      int length;
      while ((length = source.read(buffer)) > 0) {
        dest.write(buffer, 0, length);
      }
      dest.flush();
    } catch (IOException e) {
      // 打印异常堆栈跟踪
      Log.printStackTrace(e);
      return false;
    } finally {
      // 使用 try-with-resources 语句自动关闭资源
      close(source);
      close(dest);
    }
    return true;
  }

  /**
   * 安全关闭 Closeable 对象。 此方法会捕获并记录在关闭过程中可能发生的任何异常。
   *
   * @param c 要关闭的 Closeable 对象，可以是 null
   */
  public static void close(Closeable c) {
    if (c != null) {
      try {
        c.close();
      } catch (IOException e) {
        // 打印异常堆栈跟踪
        Log.printStackTrace(TAG, e);
      }
    }
  }

  /**
   * 创建文件。 如果指定的路径已经存在且为目录，则尝试删除该目录。 如果文件不存在，则创建文件，并且确保其父目录存在。
   *
   * @param file 要创建的文件
   * @return 如果文件创建成功，则返回对应的 File 对象；如果失败，则返回 null
   */
  public static File createFile(File file) {
    // 如果文件已存在且为目录，则尝试删除
    if (file.exists() && file.isDirectory()) {
      if (!file.delete()) {
        // 如果删除失败，则返回 null
        return null;
      }
    }
    // 如果文件不存在，则尝试创建
    if (!file.exists()) {
      try {
        // 获取父目录
        File parentFile = file.getParentFile();
        // 确保父目录存在，如果父目录为 null 或创建失败，则忽略异常
        if (parentFile != null && !parentFile.mkdirs()) {
          // 如果父目录创建失败，则返回 null
          return null;
        }
        // 创建文件
        if (!file.createNewFile()) {
          // 如果文件创建失败，则返回 null
          return null;
        }
      } catch (Exception e) {
        // 打印异常堆栈跟踪
        Log.printStackTrace(e);
        return null;
      }
    }
    // 返回创建或已存在的文件
    return file;
  }

  /**
   * 创建目录。 如果指定的路径已经存在且为文件，则尝试删除该文件。 如果路径不存在，则创建相应的目录结构。
   *
   * @param file 要创建的目录
   * @return 如果目录创建成功，则返回对应的 File 对象；如果失败，则返回 null
   */
  public static File createDirectory(File file) {
    // 如果文件已存在且为文件，则尝试删除
    if (file.exists() && file.isFile()) {
      if (!file.delete()) {
        // 如果删除失败，则返回 null
        return null;
      }
    }

    // 如果目录不存在，则尝试创建
    if (!file.exists()) {
      try {
        // 使用 mkdirs() 来创建多级目录
        if (!file.mkdirs()) {
          // 如果目录创建失败，则返回 null
          return null;
        }
      } catch (Exception e) {
        // 打印异常堆栈跟踪
        Log.printStackTrace(e);
        return null;
      }
    }

    // 返回创建或已存在的目录
    return file;
  }

  /**
   * 清空文件内容。
   *
   * @param file 要清空的文件
   * @return 如果文件存在且内容被成功清空，则返回 true；如果文件不存在，则返回 false
   */
  public static boolean clearFile(File file) {
    // 检查文件是否存在
    if (file.exists() && file.isFile()) {
      try (FileWriter fileWriter = new FileWriter(file)) {
        // 写入空字符串以清空文件内容
        fileWriter.write("");
        fileWriter.flush();
        return true;
      } catch (IOException e) {
        // 打印异常堆栈跟踪
        Log.printStackTrace(e);
      }
    }
    return false;
  }

  /**
   * 删除文件或目录（包括空或非空目录）。
   *
   * @param file 要删除的文件或目录
   * @return 如果文件或目录被成功删除，则返回 true；如果文件或目录不存在，则返回 false
   */
  public static boolean deleteFile(File file) {
    // 检查文件或目录是否存在
    if (!file.exists()) {
      return false;
    }
    // 如果是文件，则直接删除
    if (file.isFile()) {
      return file.delete();
    }
    // 如果是目录，先删除目录中的所有文件和子目录
    File[] files = file.listFiles();
    if (files != null) {
      for (File innerFile : files) {
        // 递归删除子文件或子目录
        deleteFile(innerFile);
      }
    }
    // 删除目录本身
    return file.delete();
  }
}
