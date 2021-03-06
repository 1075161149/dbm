package org.onetwo.common.db.spi;

import java.util.List;

import org.onetwo.common.db.ParsedSqlContext;
import org.onetwo.common.db.dquery.NamedQueryInvokeContext;
import org.onetwo.common.utils.Page;

/****
 * 基于文件的命名查询工厂
 * 各个createQuery方法最终调用 {@linkplain QueryProvideManager#createQuery}
 * @author wayshall
 *
 */
public interface FileNamedQueryFactory {
	
	/****
	 * 通过InvokeContext查找
	 * @param invokeContext
	 * @return
	 */
//	public DbmNamedQueryInfo getNamedQueryInfo(NamedQueryInvokeContext invokeContext);
	/***
	 * @return
	 */
	public NamedSqlFileManager getNamedSqlFileManager();

	public QueryWrapper createQuery(NamedQueryInvokeContext invokeContext);
	
	
	public ParsedSqlContext parseNamedQuery(NamedQueryInvokeContext invokeContext);
//	public FileNamedSqlGenerator createFileNamedSqlGenerator(NamedQueryInvokeContext invokeContext);
	
//	public DataQuery createQuery(JFishNamedFileQueryInfo nameInfo, PlaceHolder type, Object... args);

	public QueryWrapper createCountQuery(NamedQueryInvokeContext invokeContext);

	public <T> List<T> findList(NamedQueryInvokeContext invokeContext);

	public <T> T findUnique(NamedQueryInvokeContext invokeContext);
	public <T> T findOne(NamedQueryInvokeContext invokeContext);

	public <T> Page<T> findPage(Page<T> page, NamedQueryInvokeContext invokeContext);
	
	
}
