package com.github.catstiger.multipart.controller;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.github.catstiger.common.util.UUIDHex;
import com.github.catstiger.multipart.model.FileObject;
import com.github.catstiger.multipart.service.FileObjectService;
import com.github.catstiger.multipart.service.FileService;
import com.github.catstiger.multipart.service.impl.OssFileService;
import com.github.catstiger.websecure.subject.Subject;
import com.github.catstiger.websecure.user.model.User;
import com.github.catstiger.websecure.web.SubjectHolder;

/**
 * 文件上传Servlet基类
 * 
 * @author catstiger
 * 
 */
@SuppressWarnings("serial")
public class AbstractUploadServlet extends HttpServlet {
	protected Logger logger = LoggerFactory.getLogger(getClass());

	protected WebApplicationContext context;
	protected FileService fileService;
	protected FileObjectService fileObjectService;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		context = WebApplicationContextUtils.getRequiredWebApplicationContext(config.getServletContext());
		fileService = context.getBean(FileService.class);
		fileObjectService = context.getBean(FileObjectService.class);
	}

	/**
	 * 上传单个文件
	 */
	protected FileObject doUploadSingle(HttpServletRequest request, HttpServletResponse response) throws Exception {
		request.setCharacterEncoding("UTF-8");

		DiskFileItemFactory factory = new DiskFileItemFactory();
		factory.setSizeThreshold(4096);
		File repository = getRepository(request);
		factory.setRepository(repository);
		ServletFileUpload servletFileUpload = new ServletFileUpload(factory);

		List<FileItem> fileItems = servletFileUpload.parseRequest(request);
		FileObject fileObject = new FileObject();

		if (CollectionUtils.isNotEmpty(fileItems)) {
			// 提取文件对象
			for (Iterator<FileItem> itr = fileItems.iterator(); itr.hasNext();) {
				FileItem fileItem = itr.next();

				if (fileItem.getContentType() == null) {
					continue;
				}
				// 获取原文件名
				String path = fileItem.getName();
				int start = path.lastIndexOf(File.separator);
				String filename = path.substring(start + 1);
				// 扩展名
				String ext = org.springframework.util.StringUtils.getFilenameExtension(filename);
				String shortGeneratedFilename = new StringBuilder(100).append(UUIDHex.gen()).append(".").append(ext).toString();

				File targetFile = new File(repository.getPath() + File.separator + shortGeneratedFilename);
				fileItem.write(targetFile); // 写文件

				// 如存储到了云,则删除targetFile(本地没必要保存)--目前是有OssFileService,如果再增加其他云文件服务,则需要再增加判断
				if (fileService instanceof OssFileService) {
					fileObject = fileObjectService.create(targetFile, filename, SubjectHolder.getSubject().getPrincipal().getName());
					if (targetFile.exists()) {
						targetFile.delete();
					}
				} else {// 如果不是存储到云,则targetFile即为本地目标文件,仅需保存FileObject
					User user = (User) SubjectHolder.getSubject().getPrincipal();
					String userName = ((user == null) ? null : user.getUsername());
					String url = fileService.makeUrl(shortGeneratedFilename, userName);
					fileObject = fileObjectService.create(targetFile, url, filename, SubjectHolder.getSubject().getPrincipal().getName());
				}
			}
		}

		return fileObject;
	}

	protected String getExtra(HttpServletRequest request) {
		Subject subject = SubjectHolder.getSubject();
		if (subject == null) {
			return null;
		}
		User user = (User) subject.getPrincipal();
		String extra = null;
		if (user != null) {
			extra = user.getUsername();
		}
		return extra;
	}

	/**
	 * 获取文件存储位置
	 */
	protected File getRepository(HttpServletRequest request) {
		String extra = getExtra(request);
		File targetPath = new File(fileService.buildPath(extra)); // 文件存储的目标路径

		if (!targetPath.exists()) {
			targetPath.mkdirs();
		}

		return targetPath;
	}

	/**
	 * 一字符串形式得到User ID
	 */
	protected String userId() {
		Subject subject = SubjectHolder.getSubject();
		if (subject.isAuthenticated()) {
			User user = (User) subject.getPrincipal();
			return user.getId().toString();
		}

		return null;
	}

}
