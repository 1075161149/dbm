package org.onetwo.common.db.filequery;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.onetwo.common.db.AbstractDbmQueryWrapper;
import org.onetwo.common.db.DbmQueryWrapper;
import org.onetwo.common.db.ParsedSqlContext;
import org.onetwo.common.db.filequery.ParsedSqlUtils.ParsedSqlWrapper;
import org.onetwo.common.db.filequery.ParsedSqlUtils.ParsedSqlWrapper.SqlParamterMeta;
import org.onetwo.common.db.filequery.spi.FileNamedSqlGenerator;
import org.onetwo.common.db.filequery.spi.QueryProvideManager;
import org.onetwo.common.db.filequery.spi.SqlParamterPostfixFunctionRegistry;
import org.onetwo.common.db.sql.QueryOrderByable;
import org.onetwo.common.db.sqlext.ExtQueryUtils;
import org.onetwo.common.spring.SpringUtils;
import org.onetwo.common.spring.ftl.TemplateParser;
import org.onetwo.common.utils.ArrayUtils;
import org.onetwo.common.utils.Assert;
import org.onetwo.common.utils.CUtils;
import org.onetwo.common.utils.LangUtils;
import org.onetwo.dbm.dialet.DBDialect;
import org.springframework.beans.BeanWrapper;
import org.springframework.jdbc.core.RowMapper;

public class DefaultFileQueryWrapper extends AbstractDbmQueryWrapper implements QueryOrderByable {

//	private DynamicQuery query;
//	private JFishNamedFileQueryInfo info;
	protected QueryProvideManager baseEntityManager;
	private DbmQueryWrapper dataQuery;
	
	protected boolean countQuery;


	private Map<Object, Object> params = LangUtils.newHashMap();
	private int firstRecord = -1;
	private int maxRecords;
	private Class<?> resultClass;
	
	protected DbmNamedQueryInfo info;
	private TemplateParser parser;
	private ParserContext parserContext = ParserContext.create();
	
	private String[] ascFields;
	private String[] desFields;

	public DefaultFileQueryWrapper(QueryProvideManager baseEntityManager, DbmNamedQueryInfo info, boolean count, TemplateParser parser) {
		Assert.notNull(baseEntityManager);
		this.baseEntityManager = baseEntityManager;
		this.countQuery = count;
		this.parser = parser;
		
		this.info = info;
//		this.resultClass = countQuery?Long.class:info.getMappedEntityClass();
		if(countQuery){
			this.resultClass = LangUtils.isIntegralType(info.getMappedEntityClass())?info.getMappedEntityClass():Long.class;
		}else{
			this.resultClass = info.getMappedEntityClass();
		}
	}
	
//	abstract protected DataQuery createDataQuery(DynamicQuery query);
//	abstract protected DataQuery createDataQuery(String sql, Class<?> mappedClass);

	
	protected DbmQueryWrapper createDataQuery(String sql, Class<?> mappedClass){
		DbmQueryWrapper dataQuery = this.baseEntityManager.createSQLQuery(sql, mappedClass);
		return dataQuery;
	}
	
	protected DbmQueryWrapper createDataQueryIfNecessarry(){
		if(dataQuery!=null){
			return dataQuery;
		}
		DBDialect dbDialect = baseEntityManager.getSessionFactory().getDialect();
		FileNamedSqlGenerator sqlGen = new DefaultFileNamedSqlGenerator(info, countQuery, parser, getParserContext(), 
																		resultClass, ascFields, desFields, params, dbDialect);
		ParsedSqlContext sqlAndValues = sqlGen.generatSql();
		if(sqlAndValues.isListValue()){
			DbmQueryWrapper dataQuery = createDataQuery(sqlAndValues.getParsedSql(), resultClass);
			int position = 0;
			for(Object value : sqlAndValues.asList()){
				dataQuery.setParameter(position++, value);
			}
			setLimitResult();
			this.dataQuery = dataQuery;
			return dataQuery;
		}

		String parsedSql = sqlAndValues.getParsedSql();
		DbmQueryWrapper dataQuery = createDataQuery(parsedSql, resultClass);
		
		SqlParamterPostfixFunctionRegistry sqlFunc = baseEntityManager.getSessionFactory().getServiceRegistry().getSqlParamterPostfixFunctionRegistry();
		ParsedSqlWrapper sqlWrapper = ParsedSqlUtils.parseSql(parsedSql, sqlFunc);
		BeanWrapper paramBean = SpringUtils.newBeanMapWrapper(sqlAndValues.asMap());
		for(SqlParamterMeta parameter : sqlWrapper.getParameters()){
			if(!paramBean.isReadableProperty(parameter.getProperty()))
				continue;
			/*Object value = paramBean.getPropertyValue(parameterName);
			if(parameter.hasFunction()){
				value = ReflectUtils.invokeMethod(parameter.getFunction(), SqlParamterFunctions.getInstance(), value);
			}*/
			Object pvalue = parameter.getParamterValue(paramBean);
			if(pvalue!=null && sqlAndValues.getQueryConfig().isLikeQueryField(parameter.getName())){
				pvalue = ExtQueryUtils.getLikeString(pvalue.toString());
			}
			//wrap pvalue
			dataQuery.setParameter(parameter.getName(), pvalue);
		}
		
		setLimitResult();

		this.dataQuery = dataQuery;
		return dataQuery;
	}
	private void setLimitResult(){
		if(firstRecord>0)
			dataQuery.setFirstResult(firstRecord);
		if(maxRecords>0)
			dataQuery.setMaxResults(maxRecords);
	}

	public DbmQueryWrapper setParameter(int index, Object value) {
		this.params.put(index, value);
		return this;
	}

	public DbmQueryWrapper setParameter(String name, Object value) {
		JNamedQueryKey key = JNamedQueryKey.ofKey(name);
		if(key!=null){
			this.processQueryKey(key, value);
		}else{
			this.params.put(name, value);
		}
		return this;
	}

	public <T> T getSingleResult() {
		return createDataQueryIfNecessarry().getSingleResult();
	}

	public int executeUpdate() {
		return this.createDataQueryIfNecessarry().executeUpdate();
	}
	
	public <T> List<T> getResultList() {
		return createDataQueryIfNecessarry().getResultList();
	}

	public DbmQueryWrapper setFirstResult(int firstResult) {
		this.firstRecord = firstResult;
		return this;
	}

	public DbmQueryWrapper setMaxResults(int maxResults) {
		this.maxRecords = maxResults;
		return this;
	}

	public DbmQueryWrapper setResultClass(Class<?> resultClass) {
		this.resultClass = resultClass;
		return this;
	}

	public DbmQueryWrapper setParameters(Map<String, Object> params) {
		for(Entry<String, Object> entry : params.entrySet()){
			setParameter(entry.getKey(), entry.getValue());
		}
		return this;
	}

	/****
	 * 根据key类型设置参数、返回结果类型、排序……等等
	 * @param params
	 */
	public void setQueryAttributes(Map<Object, Object> params) {
		Object key;
		for(Entry<Object, Object> entry : params.entrySet()){
			key = entry.getKey();
			if(String.class.isInstance(key)){
				setParameter(key.toString(), entry.getValue());
			}else if(Integer.class.isInstance(key)){
				setParameter((Integer)key, entry.getValue());
			}else if(JNamedQueryKey.class.isInstance(key)){
				this.processQueryKey((JNamedQueryKey)key, entry.getValue());
			}
		}
	}

	private void processQueryKey(JNamedQueryKey qkey, Object value){
		switch (qkey) {
			case ResultClass:
				if(!countQuery)
					setResultClass((Class<?>)value);
				break;
			case ASC:
				String[] ascFields = CUtils.asStringArray(value);
				asc(ascFields);
				break;
			case DESC:
				String[] desFields = CUtils.asStringArray(value);
				desc(desFields);
				break;
			case ParserContext:
				this.setParserContext((ParserContext)value);
			default:
				break;
		}
		
//		if(JNamedQueryKey.ResultClass.equals(qkey) && !countQuery){//count qyery 忽略
//			setResultClass((Class<?>)value);
//		}
	}
	
	public DbmQueryWrapper setParameters(List<Object> params) {
		int position = 1;
		for(Object value : params){
			setParameter(position, value);
			position++;
		}
		return this;
	}

	@Override
	public void asc(String... fields) {
		this.ascFields = fields;
		/*if(isNeedParseSql() && QueryOrderByable.class.isAssignableFrom(query.getClass())){
			((QueryOrderByable)query).asc(fields);
		}else{
			throw new UnsupportedOperationException("the query can't supported orderby, you need set ignore.null to true.");
		}*/
	}

	@Override
	public void desc(String... fields) {
		this.desFields = fields;
	}
	

	@Override
	public DbmQueryWrapper setParameters(Object[] params) {
		if(ArrayUtils.hasNotElement(params))
			return this;
		int position = 1;
		for(Object value : params){
			setParameter(position++, value);
		}
		return this;
	}

	public DbmQueryWrapper setLimited(final Integer first, final Integer max) {
		this.firstRecord = first;
		this.maxRecords = max;
		return this;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getRawQuery(Class<T> clazz) {
		this.createDataQueryIfNecessarry();
		return (T)dataQuery;
	}
	@Override
	public DbmQueryWrapper setQueryConfig(Map<String, Object> configs) {
		return null;
	}

	public void setParserContext(ParserContext parserContext) {
		this.parserContext = parserContext;
	}

	public ParserContext getParserContext() {
		if(parserContext==null){
			parserContext = ParserContext.create();
		}
		return parserContext;
	}

	@Override
	public void setRowMapper(RowMapper<?> rowMapper) {
		this.dataQuery.setRowMapper(rowMapper);
	}

}
