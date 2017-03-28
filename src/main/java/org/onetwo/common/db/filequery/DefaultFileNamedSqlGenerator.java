package org.onetwo.common.db.filequery;

import java.util.Map;
import java.util.Map.Entry;

import org.onetwo.common.db.DataBase;
import org.onetwo.common.db.ParsedSqlContext;
import org.onetwo.common.db.filequery.spi.FileNamedSqlGenerator;
import org.onetwo.common.db.sql.DynamicQuery;
import org.onetwo.common.db.sql.DynamicQueryFactory;
import org.onetwo.common.db.sqlext.ExtQueryUtils;
import org.onetwo.common.log.JFishLoggerFactory;
import org.onetwo.common.spring.ftl.TemplateParser;
import org.onetwo.common.utils.LangUtils;
import org.onetwo.dbm.dialet.DBDialect;
import org.slf4j.Logger;

/****
 * sql语句解释
 * @author way
 *
 */
public class DefaultFileNamedSqlGenerator implements FileNamedSqlGenerator {
	public static final String PARSER_ACCESS_KEY = DbmNamedQueryInfo.FRAGMENT_KEY;
	
	private static final Logger logger = JFishLoggerFactory.getLogger(DefaultFileNamedSqlGenerator.class);
	protected DbmNamedQueryInfo info;
	protected boolean countQuery;
	private TemplateParser parser;
	private ParserContext parserContext;
	private Class<?> resultClass;
	
	private String[] ascFields;
	private String[] desFields;

	private Map<Object, Object> params;
	private final DBDialect dbDialect;
	
	
	
	public DefaultFileNamedSqlGenerator(DbmNamedQueryInfo info, boolean countQuery,
			TemplateParser parser, Map<Object, Object> params, DBDialect dbDialect) {
		super();
		this.info = info;
		this.countQuery = countQuery;
		this.parser = parser;
		this.params = LangUtils.emptyIfNull(params);
		if(params!=null){
			this.parserContext = (ParserContext)this.params.get(JNamedQueryKey.ParserContext);
		}
		this.dbDialect = dbDialect;
	}

	public DefaultFileNamedSqlGenerator(DbmNamedQueryInfo info, boolean countQuery,
			TemplateParser parser, ParserContext parserContext,
			Class<?> resultClass, String[] ascFields, String[] desFields,
			Map<Object, Object> params, DBDialect dbDialect) {
		super();
		this.info = info;
		this.countQuery = countQuery;
		this.parser = parser;
		this.parserContext = parserContext;
		this.resultClass = resultClass;
		this.ascFields = ascFields;
		this.desFields = desFields;
		this.params = LangUtils.emptyIfNull(params);
		this.dbDialect = dbDialect;
	}

	@Override
	public ParsedSqlContext generatSql(){
		String parsedSql = null;
		ParsedSqlContext sv = null;
		if(info.getFileSqlParserType()==FileSqlParserType.IGNORENULL){
			String sql = countQuery?info.getCountSql():info.getSql();
			DynamicQuery query = DynamicQueryFactory.createJFishDynamicQuery(sql, resultClass);
			for(Entry<Object, Object> entry : this.params.entrySet()){
				query.setParameter(entry.getKey().toString(), entry.getValue());
			}
			query.asc(ascFields);
			query.desc(desFields);
			query.compile();
			parsedSql = query.getTransitionSql();
			sv = new SqlAndValues(false, parsedSql, query.getValues());
			
		}else if(info.getFileSqlParserType()==FileSqlParserType.TEMPLATE){
//			Assert.notNull(parserContext);
			if(parserContext==null){
				parserContext = ParserContext.create();
			}
			
			this.parserContext.put(SqlFunctionFactory.CONTEXT_KEY, dbDialect.getSqlFunctionDialet());
			this.parserContext.putAll(params);
			FragmentTemplateParser attrParser = new FragmentTemplateParser(parser, parserContext, info);
			this.parserContext.put(PARSER_ACCESS_KEY, attrParser);
			
			if(countQuery){
				parsedSql = this.parser.parse(info.isAutoGeneratedCountSql()?info.getFullName():info.getCountName(), parserContext);
				if(info.isAutoGeneratedCountSql()){
					parsedSql = ExtQueryUtils.buildCountSql(parsedSql, "");
				}
			}else{
				parsedSql = this.parser.parse(info.getFullName(), parserContext);
			}
			sv = new SqlAndValues(true, parsedSql, params, parserContext.getQueryConfig());
			
		}else{
			parsedSql = countQuery?info.getCountSql():info.getSql();
			sv = new SqlAndValues(true, parsedSql, params);
		}
		logger.info("parsed sql : {}", parsedSql);

		return sv;
	}
	
}
