package com.github.catstiger.multipart.service;

import java.io.File;
import java.io.OutputStream;

/**
 * 用于处理上传文件的接口
 * 
 * @author catstiger@gmail.com
 *
 */
public interface FileService {
  /**
   * 返回存放文件的跟路径，末尾包括{@link java.io.File#separator}
   */
  public String getFSRoot();

  /**
   * 返回URL地址的ROOT,起始位置和结束位置包括“/”
   */
  public String getUrlRoot();

  /**
   * 根据附加码，构建文件存储的路径。以extra的hashCode作为路径生成的依据，每隔3位作为一个子路径。 例如:<br>
   * 809909544: 809/909/544 8099095447: 809/909/544/7
   * 
   * @param extra 给定的附加码，如果为空，则表示使用临时文件夹
   * @return 文件存储路径，绝对路径，末尾包括{@link java.io.File#separator}
   */
  public String buildPath(String extra);

  /**
   * 根据文件名称（上传后修改的短文件名）和附加码，生成访问该文件的URL路径
   */
  public String makeUrl(String shortFilename, String extra);

  /**
   * 保存上传的文件，并返回保存后的ID
   * 
   * @param file 上传的文件
   * @param filename 上传文件的原名
   * @param extra 附加码，通常是当前登陆用户ID，决定了文件存储的物理位置 和访问文件的URL。如果为空，则表示使用临时文件夹。
   * @return 访问该文件的URL
   */
  public String save(File file, String filename, String extra);

  /**
   * 保存上传的文件，保留原来的文件名，并返回保存后的ID
   * 
   * @param file 上传的文件
   * @param filename 上传文件的原名
   * @param extra 附加码，通常是当前登陆用户ID，决定了文件存储的物理位置 和访问文件的URL。如果为空，则表示使用临时文件夹。
   * @return 访问该文件的URL
   */
  public String saveWithOriginalName(File file, String filename, String extra);

  /**
   * 删除上传的文件
   * 
   * @param url 访问该文件的URL
   */
  public void delete(String url);

  /**
   * 删除类似的上传文件，例如，参数为/f/x/xxxx.jpg 那么/f/x/xxxxA.jpg和/f/x/xxxB.jpg也会被删除
   * 
   */
  public void delLike(String url);

  /**
   * 移动一个文件到指定用户目录
   * 
   * @param src 访问原始文件的url
   * @param extra 附加码
   * @return 访问新文件的URL
   */
  public String mv(String src, String extra);

  /**
   * 复制一个文件到指定用户目录
   * 
   * @param src 访问原始文件的url
   * @param extra 附加码
   * @return 访问新文件的URL
   */
  public String cp(String src, String extra);

  /**
   * 将指定的文件输出到OutputStream
   * 
   * @param url 访问该文件的URL
   */
  public void read(String url, OutputStream out);

  /**
   * 根据URL地址，返回文件的存储路径。
   */
  public String get(String url);

  /**
   * 根据给定的文件，找出同一个目录下文件名类似的文件。 所谓文件名类似，指的是扩展名相同，被查找的文件的名称 使用参考文件文件名后面再加一些。
   * 
   * @param file 给定的文件
   * @return
   */
  public File[] get(File file);

  /**
   * 判断给定URL所指向的文件是否存在
   */
  public boolean exists(String url);

  /**
   * 删除临时文件
   */
  public void deleteTemps();

  /**
   * 返回目标URL指向的文件的ContentLength
   * 
   * @return
   */
  public long getContentLength(String url);

}
