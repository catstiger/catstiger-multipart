package com.github.catstiger.multipart.service.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.Resource;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.FileCopyUtils;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import com.github.catstiger.common.util.ContentTypes;
import com.github.catstiger.common.util.UUIDHex;

//@Component
public class OSSFileService extends AbstractFileService {
  private static Logger logger = LoggerFactory.getLogger(OSSFileService.class);

  @Resource
  private OSSClient ossClient;

  @Autowired
  private OSSProperties ossProp;

  @Override
  public String getUrlRoot() {
    return makeOssKey(urlRoot);
  }

  @Override
  public String save(File file, String filename, String extra) {
    if (file == null || !file.exists()) {
      return null;
    }
    ObjectMetadata meta = new ObjectMetadata();
    String ext = StringUtils.lowerCase(org.springframework.util.StringUtils.getFilenameExtension(filename));
    // 必须设置ContentLength
    meta.setContentLength(file.length());
    meta.setContentType(ContentTypes.get(ext));

    InputStream in = null;
    try {
      in = new BufferedInputStream(new FileInputStream(file));
      StringBuilder randomFn = new StringBuilder(100).append(UUIDHex.gen());
      if (!StringUtils.isEmpty(ext)) {
        randomFn.append(".").append(ext);
      }

      String key = makeUrl(randomFn.toString(), extra);
      ossClient.putObject(ossProp.getBucket(), key, in, meta);

      return "/" + key;
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return null;
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

  }

  @Override
  public String saveWithOriginalName(File file, String filename, String extra) {
    if (file == null || !file.exists()) {
      return null;
    }
    ObjectMetadata meta = new ObjectMetadata();
    // 必须设置ContentLength
    meta.setContentLength(file.length());
    String ext = StringUtils.lowerCase(org.springframework.util.StringUtils.getFilenameExtension(filename));
    meta.setContentType(ContentTypes.get(ext));

    InputStream in = null;
    try {
      in = new BufferedInputStream(new FileInputStream(file));
      String key = makeUrl(filename, extra);
      logger.debug("Put to OSS {}", key);
      ossClient.putObject(ossProp.getBucket(), key, in, meta);

      return "/" + key;
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return null;
    } finally {
      try {
        if (in != null) {
          in.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

  }

  @Override
  public void delete(String url) {
    if (StringUtils.isBlank(url)) {
      return;
    }
    String key = makeOssKey(url);
    try {
      ossClient.deleteObject(ossProp.getBucket(), key);
    } catch (Exception e) {
      logger.warn(e.getMessage());
    }
  }

  @Override
  public void delLike(String url) {
    throw new java.lang.UnsupportedOperationException("暂不支持模糊删除功能。");
  }

  @Override
  public String mv(String src, String extra) {
    String key = cp(src, extra);
    delete(src);

    return key;
  }

  @Override
  public String cp(String src, String extra) {
    if (StringUtils.isBlank(src) || src.endsWith("/")) {
      return null;
    }
    String filename = src;
    int sp = src.lastIndexOf("/");
    if (sp > 0) {
      filename = src.substring(sp + 1);
    }
    String ext = StringUtils.lowerCase(org.springframework.util.StringUtils.getFilenameExtension(filename));
    StringBuilder randomFn = new StringBuilder(100).append(UUIDHex.gen());
    if (!StringUtils.isEmpty(ext)) {
      randomFn.append(".").append(ext);
    }

    String key = makeUrl(randomFn.toString(), extra);
    try {
      ossClient.copyObject(ossProp.getBucket(), makeOssKey(src), ossProp.getBucket(), makeOssKey(key));
    } catch (Exception e) {
      logger.error(e.getMessage());
    }

    return "/" + key;
  }

  @Override
  public void read(String url, OutputStream out) {
    if (StringUtils.isBlank(url)) {
      return;
    }

    try {
      String key = makeOssKey(url);
      logger.debug("Reading oss object {}", key);
      OSSObject ossObject = ossClient.getObject(ossProp.getBucket(), key);
      InputStream in = ossObject.getObjectContent();
      FileCopyUtils.copy(in, out);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public String get(String url) {
    if (StringUtils.isBlank(url)) {
      return StringUtils.EMPTY;
    }

    File path = new File(new StringBuilder(50).append(getFSRoot()).append(TEMP_FOLDER).toString());
    if (!path.exists()) {
      path.mkdirs();
    }

    String key = makeOssKey(url);
    String filename = key;
    if (StringUtils.indexOf(filename, "/") >= 0) {
      filename = filename.substring(StringUtils.lastIndexOf(filename, "/") + 1);
    }

    String file = new StringBuilder(100).append(path.getAbsolutePath()).append(File.separator).append(filename).toString();
    // 将OSS中得文件复制到本地，才能调用本方法
    try {
      read(key, new BufferedOutputStream(new FileOutputStream(new File(file))));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return StringUtils.EMPTY;
    }

    return file;
  }

  @Override
  public File[] get(File file) {
    throw new java.lang.UnsupportedOperationException("OSS不支持此操作。");
  }

  @Override
  public boolean exists(String url) {
    if (StringUtils.isBlank(url)) {
      return false;
    }
    String key = makeOssKey(url);
    try {
      ossClient.getObject(ossProp.getBucket(), key);
      return true;
    } catch (Exception e) {
      return false;
    }
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

  public void setOssClient(OSSClient ossClient) {
    this.ossClient = ossClient;
  }

  /**
   * 将URL地址转换为符合OSS要求的Key
   * 
   * @param url 原始URL地址
   * @return
   */
  private String makeOssKey(String url) {
    if (StringUtils.isBlank(url)) {
      return StringUtils.EMPTY;
    }

    int pro = StringUtils.indexOf(url, "//");
    if (pro >= 0) {
      url = url.substring(pro + 2);
    }
    if (url.indexOf("/") == 0) {
      url = url.substring(url.indexOf("/") + 1);
    }

    return url;
  }

  @Override
  public long getContentLength(String url) {
    if (StringUtils.isBlank(url)) {
      return 0L;
    }
    String key = makeOssKey(url);
    try {
      OSSObject object = ossClient.getObject(ossProp.getBucket(), key);
      return object.getObjectMetadata().getContentLength();
    } catch (Exception e) {
      return 0L;
    }
  }

  /**
   * 将指定目录下的所有文件全部导入OSS
   * 
   * @param dir 指定一个目录，该目录下的文件将全部导入OSS，原来的路径不变
   * @param prefix OSS中的前缀，不可以以 "/"， "\"开头
   * @param exclusives 被排除的目录，必须以"/"开头
   */
  public void putObjects(File dir, String prefix, String[] exclusives) {
    if (StringUtils.isBlank(prefix)) {
      prefix = "f";
    }

    if (prefix.startsWith("/")) {
      prefix = prefix.substring(1);
    }

    if (prefix.endsWith("/")) {
      prefix = prefix.substring(0, prefix.length() - 1);
    }
    Map<String, File> keyFiles = new TreeMap<String, File>();
    searchFiles(dir, dir, keyFiles);

    long length = 0L;
    int index = 0;
    Set<String> keys = keyFiles.keySet();
    for (String key : keys) {
      if (isExclusive(key, exclusives)) {
        continue;
      }

      String objectKey = prefix + key;
      File file = keyFiles.get(key);
      putObject(file, objectKey);

      index++;
      length += file.length();

      if (index % 100 == 0) {
        logger.info("导入个{}文件, 共{}", index, getSizeString(length));
      }
    }
    logger.info("导入完成！");
  }

  /**
   * 初始导入文件
   */
  public void initFile() {
    File dir = new File("/mnt/121f/.jtiger/");
    putObjects(dir, "/f/", new String[] { "/cms_index", "/LSOSO_INDEX_LOCAL", "/temp_" });
  }

  /**
   * 将一个文件上传的OSS服务器
   * 
   * @param file 给定文件
   * @param key 给定文件在OSS服务器上的key，可以包括路径，但是不能以"/"或者"\"开头
   */
  public void putObject(File file, String key) {
    if (file == null || !file.exists()) {
      return;
    }
    if (StringUtils.isBlank(key)) {
      key = "f";
    }

    if (key.startsWith("/")) {
      key = key.substring(1);
    }
    ObjectMetadata meta = new ObjectMetadata();
    meta.setContentLength(file.length());
    String ext = StringUtils.lowerCase(org.springframework.util.StringUtils.getFilenameExtension(file.getName()));
    meta.setContentType(ContentTypes.get(ext));
    InputStream input = null;
    try {
      input = new BufferedInputStream(new FileInputStream(file));
      ossClient.putObject(ossProp.getBucket(), key, input, meta);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } finally {
      if (input != null) {
        try {
          input.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

  }

  private boolean isExclusive(String key, String[] exclusives) {
    boolean exclusive = false;

    for (String exc : exclusives) {
      if (StringUtils.indexOf(key, exc) == 0) {
        return true;
      }
    }

    return exclusive;
  }

  private void searchFiles(File rootDir, File parentDir, Map<String, File> keyFiles) {
    if (parentDir == null) {
      return;
    }
    File[] files = parentDir.listFiles();
    if (files == null || files.length == 0) {
      return;
    }
    for (File file : files) {
      if (file.isFile()) {
        String key = StringUtils.replace(file.getAbsolutePath(), rootDir.getAbsolutePath(), "");
        if (isWindows()) {
          key = StringUtils.replace(key, "\\", "/");
        }
        if (!key.startsWith("/")) {
          key = "/" + key;
        }

        keyFiles.put(key, file);
      } else if (file.isDirectory()) {
        searchFiles(rootDir, file, keyFiles);
      }
    }
  }
  
  /**
   * 判断是否是Ms Windows操作系统
   */
  private static boolean isWindows() {
    String os = System.getProperty("os.name");
    return (os != null && os.toLowerCase().indexOf("windows") >= 0);
  }

  public OSSClient getOssClient() {
    return ossClient;
  }

  public String getBucket() {
    return ossProp.getBucket();
  }

}
