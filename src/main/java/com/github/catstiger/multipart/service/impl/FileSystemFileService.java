package com.github.catstiger.multipart.service.impl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.util.StringUtils;

import com.github.catstiger.common.util.Exceptions;
import com.github.catstiger.common.util.FileChannelUtil;
import com.github.catstiger.common.util.UUIDHex;
import com.github.catstiger.multipart.service.FileService;

/**
 * 使用本地文件系统实现@{link FileServer}
 * 
 * @author catstiger@gmail.com
 *
 */
//@Component
public class FileSystemFileService extends AbstractFileService {

  private FileChannelUtil fileChannelUtil = new FileChannelUtil();

  /**
   * 启动的时候，判断旧的目录(jtiger)是否存在，如果存在,则替换成新的目录(.jtiger)
   */
  @PostConstruct
  public void init() {
  }

  /**
   * @see FileService#save(File, String)
   */
  @Override
  public String save(File file, String filename, String extra) {
    if (file == null || !file.exists()) {
      logger.warn("No uploaded file found. {}", file);
    }
    File path = new File(buildPath(extra));
    logger.debug("Generated path : {}", path.getAbsolutePath());
    if (!path.exists()) {
      if (!path.mkdirs()) {
        logger.error("Build path failed '{}'", path);
        throw Exceptions.unchecked("Build path failed :" + path);
      }
    }
    // 生成的短文件名
    String ext = StringUtils.getFilenameExtension(filename);
    StringBuilder strBuilder = new StringBuilder(100).append(UUIDHex.gen());
    if (!StringUtils.isEmpty(ext)) {
      strBuilder.append(".").append(ext);
    }
    String shortGeneratedFilename = strBuilder.toString();
    // 生成的文件名和路径
    String generatedFilename = new StringBuilder(255).append(path.getAbsolutePath()).append(File.separator).append(shortGeneratedFilename).toString();
    logger.debug("New file will be upload to '{}'", generatedFilename);

    try {
      fileChannelUtil.write(new File(generatedFilename), new BufferedInputStream(new FileInputStream(file)));
    } catch (FileNotFoundException e) {
      logger.error("No uploaded file found '{}'", file.getAbsolutePath());
    }

    return makeUrl(shortGeneratedFilename, extra);
  }

  @Override
  public String saveWithOriginalName(File file, String filename, String extra) {
    if (file == null || !file.exists()) {
      logger.warn("No uploaded file found. {}", file);
    }
    File path = new File(buildPath(extra));
    logger.debug("Generated path : {}", path.getAbsolutePath());
    if (!path.exists()) {
      if (!path.mkdirs()) {
        logger.error("Build path failed '{}'", path);
        throw Exceptions.unchecked("Build path failed :" + path);
      }
    }

    // 生成的文件名和路径
    String generatedFilename = buildUnduplFilename(path.getAbsolutePath(), filename);
    logger.debug("New file will be upload to '{}'", generatedFilename);

    try {
      fileChannelUtil.write(new File(generatedFilename), new BufferedInputStream(new FileInputStream(file)));
    } catch (FileNotFoundException e) {
      logger.error("No uploaded file found '{}'", file.getAbsolutePath());
    }

    return makeUrl(new File(generatedFilename).getName(), extra);
  }

  private String buildUnduplFilename(String path, String filename) {
    String name = filename.indexOf(".") > 0 && filename.indexOf(".") < filename.length() - 1 ? filename.substring(0, filename.lastIndexOf(".")) : filename;
    String ext = filename.indexOf(".") > 0 && filename.indexOf(".") < filename.length() - 1
        ? filename.substring(filename.lastIndexOf(".") + 1, filename.length())
        : null;

    String fn = new StringBuilder(255).append(path).append(File.separator).append(filename).toString();
    File file = new File(fn);
    int index = 0;
    while (file.exists()) {
      index++;
      if (ext != null) {
        fn = new StringBuilder(255).append(path).append(File.separator).append(name).append("(").append(index).append(")").append(".").append(ext).toString();
      } else {
        fn = new StringBuilder(255).append(path).append(File.separator).append(name).append("(").append(index).append(")").toString();
      }

      file = new File(fn);
    }

    return file.getAbsolutePath();
  }

  /**
   * @see FileService#delete(String, Boolean)
   */
  @Override
  public void delete(String url) {
    if (!StringUtils.hasLength(url)) {
      return;
    }
    File file = new File(get(url));
    if (file.exists() && file.isFile()) {
      if (!file.delete()) {
        logger.warn("File '{}' cant be deleted.", file.getAbsolutePath());
      } else {
        logger.warn("File '{}' has been deleted.", file.getAbsolutePath());
      }
    }
  }

  /**
   * @see FileService#read(String, OutputStream)
   */
  @Override
  public void read(String url, OutputStream out) {
    File file = null;
    try {
      file = new File(get(URLDecoder.decode(url, "UTF-8")));
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      return;
    }
    if (!file.exists() || !file.isFile()) {
      logger.error("File of url '{}', is not found.", url);
      return;
    }
    try {
      fileChannelUtil.read(file, out);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        out.flush();
        out.close();
      } catch (IOException e) {
        e.printStackTrace();
      }

    }
  }

  @Override
  public boolean exists(String url) {
    File file = new File(get(url));
    return file.exists() && file.isFile();
  }

  @Override
  public void deleteTemps() {
    String path = buildPath(null); // 得到临时文件夹
    logger.info("Clear temp path {}", path);
    File temp = new File(path);
    if (!temp.exists() || !temp.isDirectory()) {
      return;
    }
    File[] filesInTemp = temp.listFiles();
    if (!ArrayUtils.isEmpty(filesInTemp)) {
      for (int i = 0; i < filesInTemp.length; i++) {
        filesInTemp[i].deleteOnExit();
      }
    }
    logger.info("{} files in temp folder has deleted.", filesInTemp.length);
  }

  @Override
  public File[] get(File file) {
    if (file == null || !file.exists() || file.isDirectory()) {
      return new File[] {};
    }
    File dir = file.getParentFile();
    final String filename = file.getName();
    final String ext = StringUtils.getFilenameExtension(filename);

    File[] files = dir.listFiles(new FileFilter() {
      @Override
      public boolean accept(File f) {
        if (!f.exists() || f.isDirectory()) {
          return false;
        }
        String name = f.getName();
        // 首先判断扩展名
        String ex = StringUtils.getFilenameExtension(name);
        if (!org.apache.commons.lang3.StringUtils.equalsIgnoreCase(ex, ext)) {
          return false;
        }
        // 得到被判定文件的名字，不包括扩展名
        if (StringUtils.hasText(ex)) {
          name = name.substring(0, name.lastIndexOf("."));
        }
        // 得到参考文件的名字，不包括扩展名
        String pureName = filename;
        if (StringUtils.hasText(ext)) {
          pureName = filename.substring(0, filename.lastIndexOf("."));
        }
        // 被判定文件必须以参考文件的文件名开头
        if (!name.startsWith(pureName)) {
          return false;
        }

        return true;
      }
    });
    return (ArrayUtils.isEmpty(files)) ? new File[] {} : files;
  }

  @Override
  public void delLike(String url) {
    if (!StringUtils.hasText(url)) {
      return;
    }
    File file = new File(get(url));
    File[] files = get(file);
    int l = files.length;
    for (int i = 0; i < l; i++) {
      files[i].delete();
    }
  }

  @Override
  public String mv(String src, String extra) {
    File srcFile = new File(get(src));
    String url = save(srcFile, srcFile.getName(), extra);
    delete(src);
    return url;
  }

  @Override
  public String cp(String src, String extra) {
    File srcFile = new File(get(src));
    String url = save(srcFile, srcFile.getName(), extra);
    return url;
  }

  @Override
  public long getContentLength(String url) {
    File file = new File(get(url));
    return (file.exists()) ? file.length() : 0L;
  }

}
