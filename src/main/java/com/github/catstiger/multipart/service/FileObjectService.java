package com.github.catstiger.multipart.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Table;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.catstiger.common.sql.BaseEntity;
import com.github.catstiger.common.sql.SQLExecutor;
import com.github.catstiger.common.sql.SQLReady;
import com.github.catstiger.common.sql.SQLRequest;
import com.github.catstiger.common.sql.mapper.Mappers;
import com.github.catstiger.common.util.Exceptions;
import com.github.catstiger.multipart.model.FileObject;
import com.github.catstiger.multipart.service.impl.AbstractFileService;
import com.google.common.base.Preconditions;

@Service
public class FileObjectService{
	private static Logger logger = LoggerFactory.getLogger(FileObjectService.class);
	
	@Autowired
	private SQLExecutor sqlExecutor;
	@Autowired
	private FileService fileService;
	

	/**
	 * 将文件存储，并保存一个FileObject对象，如果文件为图片文件，则压缩图片
	 * 
	 * @param file      需要保存的文件
	 * @param filename  文件原名
	 * @param extraMsg  附加信息，通常是当前登录人的用户名，用于构建目录，可以为<code>null</code>， 此时文件将存储在临时目录
	 *
	 */
	@Transactional
	public FileObject create(File file, String filename, String extraMsg){
		if (file == null || !file.exists() || !file.isFile() || file.length() <= 0L) {
			throw Exceptions.unchecked("请上传一个正确的文件！");
		}

		String url = fileService.save(file, filename, extraMsg);
		return createFileObject(file, url, filename, extraMsg);
	}

	/**
	 * 保存一个FileObject对象(仅创建FileObject实例,而不作文件保存操作)
	 * 
	 * @param file      需要保存的文件
	 * @param url       文件url
	 * @param filename  文件原名
	 * @param extraMsg  附加信息，通常是当前登录人的用户名，用于构建目录，可以为<code>null</code>， 此时文件将存储在临时目录
	
	 * @return
	 */
	@Transactional
	public FileObject create(File file, String url, String filename, String extraMsg){
		if (file == null || !file.exists() || !file.isFile() || file.length() <= 0L) {
			throw Exceptions.unchecked("请上传一个正确的文件！");
		}

		return createFileObject(file, url, filename, extraMsg);
	}

	/**
	 * 根据宿主类和宿主对象ID，查询FileObject
	 * 
	 * @param targetClass 宿主类名
	 * @param targetId    宿主ID
	 * @return
	 */
	public List<FileObject> listByOwner(Class<?> targetClass, Long targetId){
		Preconditions.checkNotNull(targetClass);
		Preconditions.checkNotNull(targetId);

		SQLReady sql = new SQLRequest(FileObject.class, true).select()
		    .append(" WHERE target_id = ?", targetId.toString())
		    .append(" AND(target_class=? OR target_class=? ",
				new Object[] { targetClass.getName(), targetClass.getSimpleName() });

		// 兼容使用表名作为targetClass
		Table table = targetClass.getAnnotation(Table.class);
		if (table != null && table.name() != null && table.name().toUpperCase().startsWith("T_")) {
			sql.append(" OR target_class=? ", StringUtils.replace(table.name().toUpperCase(), "T_", ""));
		}
		sql.append(") order by name asc");
		return sqlExecutor.query(sql.getSql(), new BeanPropertyRowMapper<FileObject>(FileObject.class), sql.getArgs());
	}

	/**
	 * 根据宿主类名(字符串)和宿主对象ID，查询FileObject
	 * 
	 * @param targetClass 宿主类名(字符串)
	 * @param targetId    宿主ID
	 * @return
	 */
	public List<FileObject> listByOwner(String targetClass, String targetId){
		Preconditions.checkNotNull(targetClass);
		Preconditions.checkNotNull(targetId);
		SQLReady sql = new SQLRequest(FileObject.class, true).select()
		    .where("target_id = ? AND target_class=?", true, targetId, targetClass)
		    .orderBy("name", SQLReady.ASC);
		return sqlExecutor.query(sql.getSql(), new BeanPropertyRowMapper<FileObject>(FileObject.class), sql.getArgs());
	}

	/**
	 * 根据宿主对象，列出关联的FileObject，<strong>这里取宿主对象的Class的SimpleName作为TargetClass参数</strong>
	 * 
	 * @param targetEntity 宿主对象
	 * @return
	 */
	public List<FileObject> listByTarget(BaseEntity targetEntity){
		Preconditions.checkArgument(targetEntity != null && targetEntity.getId() != null, "Entity id is required.");
		Long targetId = targetEntity.getId();

		SQLReady sql = new SQLRequest(FileObject.class, true).select()
		    .where(" target_id = ?", targetId.toString())
		    .and(" target_class=? ", targetEntity.getClass().getSimpleName());
		
		return sqlExecutor.query(sql.getSql(), new BeanPropertyRowMapper<FileObject>(FileObject.class), sql.getArgs());
	}

	/**
	 * 根据宿主对象，删除关联的FileObject，<strong>这里取宿主对象的Class的SimpleName作为TargetClass参数</strong>
	 * 
	 * @param targetEntity 宿主对象
	 * @return
	 */
	public void removeByTarget(BaseEntity targetEntity){
		List<FileObject> fos = listByTarget(targetEntity);
		if (CollectionUtils.isNotEmpty(fos)) {
			for (FileObject fo : fos) {
				fileService.delete(fo.getUrl());
			}
		}
		sqlExecutor.update("delete from file_object where target_class=? and target_id=?", 
		    targetEntity.getClass().getSimpleName(), targetEntity.getId());
	}

	/**
	 * 根据文件的所有者（target class和target id ）删除稳
	 * 
	 * @param targetClass 目标类别
	 * @param targetId    目标ID
	 */
	public void removeFilesByOwner(Class<?> targetClass, Long targetId){
		Preconditions.checkNotNull(targetClass);
		Preconditions.checkNotNull(targetId);

		List<FileObject> fos = listByOwner(targetClass, targetId);
		if (CollectionUtils.isNotEmpty(fos)) {
			for (FileObject fo : fos) {
				fileService.delete(fo.getUrl());
			}
		}
		sqlExecutor.update("delete from file_object where  (target_class=? or target_class=?) and target_id=?", 
		    targetClass.getSimpleName(), targetClass.getName(), targetId.toString());
	}

	/**
	 * 将一个宿主的图片，Copy到另一个宿主
	 * 
	 * @param sourceClass 宿主Class
	 * @param sourceId    宿主ID
	 * @param targetClass 目标宿主Class
	 * @param targetId    目标宿主ID
	 * @param targetExtra 目标extra
	 * @return 目标宿主的文件
	 * @deprecated 没啥卵用
	 */
	@Deprecated
	public List<FileObject> copyFilesByOwners(Class<?> sourceClass, Long sourceId, Class<?> targetClass, Long targetId, String targetExtra){
		List<FileObject> fos = listByOwner(sourceClass, sourceId);
		List<FileObject> targetFos = new ArrayList<FileObject>();

		if (CollectionUtils.isNotEmpty(fos)) {
			for (FileObject fo : fos) {
				String url = fileService.cp(fo.getUrl(), targetExtra);
				FileObject targetFo = new FileObject();
				targetFo.setUrl(url);
				targetFo.setTargetClass(targetClass.getSimpleName());
				targetFo.setTargetId(targetId.toString());
				targetFo.setName(fo.getName());
				targetFo.setExt(fo.getExt());
				targetFo.setSize(fo.getSize());
				targetFo.setSizeStr(fo.getSizeStr());
				SQLReady sql = new SQLRequest(FileObject.class).entity(targetFo).insertNonNull();
				sqlExecutor.update(sql.getSql(), sql.getArgs());

				targetFos.addAll(targetFos);
			}
		}

		return targetFos;
	}

	/**
	 * 将一组FileObject，Copy到另一个宿主，对应的文件也Copy一个副本
	 * 
	 * @param sourceFileObjects 一组文件对象
	 * @param targetClass       目标宿主Class
	 * @param targetId          目标宿主ID
	 * @param targetExtra       目标extra
	 * @return 目标宿主的文件
	 * @deprecated 没啥卵用
	 */
	@Deprecated
	public List<FileObject> copyFilesByOwners(List<FileObject> sourceFileObjects, Class<?> targetClass, Long targetId, String targetExtra){
		List<FileObject> targetFos = new ArrayList<FileObject>();

		if (CollectionUtils.isNotEmpty(sourceFileObjects)) {
			for (FileObject fo : sourceFileObjects) {
				String url = fileService.cp(fo.getUrl(), targetExtra);
				FileObject targetFo = new FileObject();
				targetFo.setUrl(url);
				targetFo.setTargetClass(targetClass.getSimpleName());
				targetFo.setTargetId(targetId.toString());
				targetFo.setName(fo.getName());
				targetFo.setExt(fo.getExt());
				targetFo.setSize(fo.getSize());
				targetFo.setSizeStr(fo.getSizeStr());
				SQLReady sql = new SQLRequest(FileObject.class).entity(targetFo).insertNonNull();
				sqlExecutor.update(sql.getSql(), sql.getArgs());

				targetFos.addAll(targetFos);
			}
		}

		return targetFos;
	}

	/**
	 * 批量设置FileObject对象的TargetID字段
	 * 
	 * @param urls     FileObject的URL，多个，用comma分割
	 * @param targetId The target ID
	 */
	@Transactional
	public void saveFiles(String urls, String targetId){
		if (StringUtils.isBlank(targetId)) {
			throw Exceptions.unchecked("需提供宿主ID");
		}
		if (StringUtils.isNotBlank(urls)) {
			String[] ids = StringUtils.split(urls, ",");

			for (String id : ids) {
				logger.debug("Adjoin file {}, {}", id, targetId);
				sqlExecutor.update("update file_object set target_id=? where url=?", targetId, id);
			}
		}
	}

	/**
	 * 将一个FileObject对象与特定的实体类的实例关联，FileObject对象的targetClass字段为实体类的{@link Class#getSimpleName()},
	 * targetId字段为实体类的{@link BaseEntity#getId()}
	 * 
	 * @param url    给出FileObject对象的url
	 * @param entity 给出实体类，ID必须存在
	 */
	@Transactional
	public void attachEntity(String url, BaseEntity entity){
		Preconditions.checkArgument(StringUtils.isNotBlank(url));
		Preconditions.checkArgument(entity != null && entity.getId() != null, "Entity id is required.");

		sqlExecutor.update("update file_object set target_id=?, target_class=? where url=?",
		    entity.getId().toString(), entity.getClass().getSimpleName(), url);
	}

	/**
	 * 将一个FileObject对象与特定的实体类的实例关联，FileObject对象的targetClass字段为实体类的{@link Class#getSimpleName()},
	 * targetId字段为实体类的{@link BaseEntity#getId()}
	 * 
	 * @param url         给出FileObject对象的url
	 * @param targetClass 实体类名
	 * @param targetId    实体实例id
	 */
	@Transactional
	public void attachTarget(String url, String targetClass, String targetId){
		Preconditions.checkArgument(StringUtils.isNotBlank(url));
		Preconditions.checkArgument(StringUtils.isNotBlank(targetClass));
		Preconditions.checkArgument(StringUtils.isNotBlank(targetId));

		sqlExecutor.update("update file_object set target_id=?, target_class=? where url=?", targetId, targetClass, url);
	}

	/**
	 * 删除一个文件，和它的文件对象
	 */
	public void remove(String url){
		fileService.delete(url);
		sqlExecutor.update("delete from file_object where url=?", url);
	}

	/**
	 * 根据URL获取一个FileObject
	 * 
	 * @param url 给出URL
	 */
	public FileObject get(String url){
	  SQLReady sqlReady = SQLReady.select(FileObject.class).where("url=?", url);
		return sqlExecutor.first(sqlReady.getSql(), Mappers.byClass(FileObject.class), sqlReady.getArgs());
	}

	public FileService getFileService(){
		return fileService;
	}

	/**
	 * 保存FileObject实体实例
	 * 
	 * @param file      文件
	 * @param url       访问文件的url
	 * @param filename  文件名
	 * @param userName  当前用户
	
	 * @return
	 */
	private FileObject createFileObject(File file, String url, String filename, String extraMsg){
		FileObject fileObject = new FileObject();
		fileObject.setUrl(url);
		fileObject.setName(filename);
		fileObject.setCreator(extraMsg);
		fileObject.setExt(org.springframework.util.StringUtils.getFilenameExtension(url));
		fileObject.setSize(file.length());
		fileObject.setSizeStr(AbstractFileService.getSizeString(file.length()));
		//fileObject.setTargetClass(targetCls);
		//fileObject.setTargetId(targetId);
		fileObject.setCreateTime(new Date());

		SQLReady sql = new SQLRequest(FileObject.class, true).entity(fileObject).insertNonNull();
		sqlExecutor.update(sql.getSql(), sql.getArgs());

		return fileObject;
	}

	/**
	 * 根据宿主对象，列出关联的所有url，<strong>这里取宿主对象的Class的SimpleName作为TargetClass参数</strong>
	 * 
	 * @param targetEntity 宿主对象
	 * @return
	 */
	public List<String> listUrlByTarget(BaseEntity targetEntity){
		Preconditions.checkArgument(targetEntity != null && targetEntity.getId() != null, "Entity id is required.");
		Long targetId = targetEntity.getId();
		SQLReady sqlReady = new SQLReady("select url from file_object where target_id = ? and target_class = ?", new Object[] { targetId, targetEntity.getClass().getSimpleName() });
		return sqlExecutor.query(sqlReady.getSql(), String.class, new Object[] { targetId, targetEntity.getClass().getSimpleName() });
	}
}
