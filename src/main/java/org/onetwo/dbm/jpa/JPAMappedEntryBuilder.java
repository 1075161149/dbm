package org.onetwo.dbm.jpa;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.onetwo.common.annotation.AnnotationInfo;
import org.onetwo.common.reflect.Intro;
import org.onetwo.common.reflect.ReflectUtils;
import org.onetwo.common.utils.JFishProperty;
import org.onetwo.common.utils.LangUtils;
import org.onetwo.common.utils.StringUtils;
import org.onetwo.dbm.core.spi.DbmInnerServiceRegistry;
import org.onetwo.dbm.dialet.AbstractDBDialect.StrategyType;
import org.onetwo.dbm.exception.DbmException;
import org.onetwo.dbm.mapping.AbstractMappedField;
import org.onetwo.dbm.mapping.BaseColumnInfo;
import org.onetwo.dbm.mapping.ColumnInfo;
import org.onetwo.dbm.mapping.DbmMappedEntry;
import org.onetwo.dbm.mapping.DbmMappedEntryBuilder;
import org.onetwo.dbm.mapping.DbmMappedEntryImpl;
import org.onetwo.dbm.mapping.DbmMappedField;
import org.onetwo.dbm.mapping.IdGeneratorFactory;
import org.onetwo.dbm.mapping.TableInfo;
import org.onetwo.dbm.mapping.version.DateVersionableType;
import org.onetwo.dbm.mapping.version.IntegerVersionableType;
import org.onetwo.dbm.mapping.version.LongVersionableType;
import org.onetwo.dbm.mapping.version.VersionableType;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;

public class JPAMappedEntryBuilder extends DbmMappedEntryBuilder {
	
	private static final Map<Class<?>, VersionableType<? extends Object>> versionTypes;
	static {
		Map<Class<?>, VersionableType<? extends Object>> tem = LangUtils.newHashMap(5);
		tem.put(int.class, new IntegerVersionableType());
		tem.put(Integer.class, new IntegerVersionableType());
		tem.put(long.class, new LongVersionableType());
		tem.put(Long.class, new LongVersionableType());
		tem.put(Date.class, new DateVersionableType());
		
		versionTypes = Collections.unmodifiableMap(tem);
	}
	
	public JPAMappedEntryBuilder(DbmInnerServiceRegistry serviceRegistry) {
		super(serviceRegistry);
	}

	@Override
	protected AbstractMappedField newMappedField(DbmMappedEntry entry, JFishProperty prop){
		AbstractMappedField mfield = null;
		if(prop.hasAnnotation(ManyToOne.class)){
			mfield = this.newManyToOneField(entry, prop);
		}else if(prop.hasAnnotation(OneToMany.class)){
			mfield = this.newOneToManyField(entry, prop);
		}else if(prop.hasAnnotation(ManyToMany.class)){
			mfield = this.newManyToManyField(entry, prop);
		}else if(prop.hasAnnotation(OneToOne.class)){
			mfield = this.newOneToOneField(entry, prop);
		}else{
			mfield = super.newMappedField(entry, prop);
		}
		return mfield;
	}
	
	protected AbstractMappedField newManyToOneField(DbmMappedEntry entry, JFishProperty prop){
		throw new UnsupportedOperationException("unsupported many to one : " + entry.getEntityClass());
	}
	
	protected AbstractMappedField newManyToManyField(DbmMappedEntry entry, JFishProperty prop){
		throw new UnsupportedOperationException("unsupported many to many : " + entry.getEntityClass());
	}
	
	protected AbstractMappedField newOneToManyField(DbmMappedEntry entry, JFishProperty prop){
		throw new UnsupportedOperationException("unsupported one to many : " + entry.getEntityClass());
	}
	
	protected AbstractMappedField newOneToOneField(DbmMappedEntry entry, JFishProperty prop){
		throw new UnsupportedOperationException("unsupported one to one : " + entry.getEntityClass());
	}

	@Override
	public boolean isSupported(Object entity){
		if(MetadataReader.class.isInstance(entity)){
			MetadataReader metadataReader = (MetadataReader) entity;
			AnnotationMetadata am = metadataReader.getAnnotationMetadata();
			return am.hasAnnotation(Entity.class.getName());
		}else{
			Class<?> entityClass = ReflectUtils.getObjectClass(entity);
			return entityClass.getAnnotation(Entity.class)!=null;
		}
	}
	

	@Override
	public DbmMappedEntry buildMappedEntry(Object object) {
		Object entity = LangUtils.getFirst(object);
		Class<?> entityClass = ReflectUtils.getObjectClass(entity);
		Optional<Field> idField = Intro.wrap(entityClass).getAllFields()
								.stream()
								.filter(f->f.getAnnotation(Id.class)!=null)
								.findAny();
							
		return buildMappedEntry(entityClass, !idField.isPresent());
	}
	
	@Override
	protected DbmMappedEntry createDbmMappedEntry(AnnotationInfo annotationInfo) {
		TableInfo tableInfo = newTableInfo(annotationInfo);
		DbmMappedEntryImpl entry = new DbmMappedEntryImpl(annotationInfo, tableInfo, serviceRegistry);
		entry.setSqlBuilderFactory(this.getDialect().getSqlBuilderFactory());
		return entry;
	}

	@Override
	protected boolean ignoreMappedField(JFishProperty field){
		return super.ignoreMappedField(field) || field.hasAnnotation(Transient.class);
	}

	@Override
	protected String buildTableName(AnnotationInfo annotationInfo){
		Table table = (Table) annotationInfo.getAnnotation(Table.class);
		String tname = table.name();
		return tname;
	}

	@Override
	protected String buildSeqName(AnnotationInfo annotationInfo, TableInfo tableInfo){
		String sname = null;
		Class<?> entityClass = annotationInfo.getSourceClass();
		SequenceGenerator sg = entityClass.getAnnotation(SequenceGenerator.class);
		if(sg!=null){
			sname = sg.sequenceName();
			if(StringUtils.isBlank(sname))
				sname = sg.name();
		}else{
			sname = super.buildSeqName(annotationInfo, tableInfo);
		}
		return sname;
	}

	@Override
	protected void buildMappedField(DbmMappedField mfield){
		if(mfield.getPropertyInfo().hasAnnotation(Id.class)){
			mfield.setIdentify(true);
			this.buildIdGeneratorsOnField(mfield);
		}
		if(mfield.getPropertyInfo().hasAnnotation(Version.class)){
			if(!versionTypes.containsKey(mfield.getPropertyInfo().getType())){
				throw new DbmException("the type of field["+mfield.getName()+"] is not a supported version type. supported types: " + versionTypes.keySet());
			}
			mfield.setVersionableType(versionTypes.get(mfield.getPropertyInfo().getType()));
		}
	}
	
	/***
	 * @author wayshall
	 * @param mfield
	 */
	@Override
	protected void buildIdGeneratorsOnField(DbmMappedField mfield){
		super.buildIdGeneratorsOnField(mfield);
		GeneratedValueIAttrs generatedValueIAttrs = mfield.getGeneratedValueIAttrs();
		
		IdGeneratorFactory.createSequenceGenerator(mfield.getPropertyInfo().getAnnotationInfo())
							.ifPresent(idGenerator->mfield.addIdGenerator(idGenerator));
		IdGeneratorFactory.createTableGenerator(mfield.getPropertyInfo().getAnnotationInfo())
							.ifPresent(idGenerator->mfield.addIdGenerator(idGenerator));
		
		GenerationType type = generatedValueIAttrs.getGenerationType();
		if(generatedValueIAttrs!=null){
			if(type==GenerationType.AUTO){
				if(this.getDialect().getDbmeta().isMySQL()){
					mfield.setStrategyType(StrategyType.INCREASE_ID);
				}else{
					mfield.setStrategyType(StrategyType.SEQ);
				}
			}else if(type==GenerationType.IDENTITY){
				mfield.setStrategyType(StrategyType.INCREASE_ID);
			}else if(type==GenerationType.SEQUENCE){
				mfield.setStrategyType(StrategyType.SEQ);
			}
		}
	}
	
	@Override
	protected BaseColumnInfo buildColumnInfo(TableInfo tableInfo, DbmMappedField field){
//		Method method = field.getReadMethod();
//		Method method = ReflectUtils.findMe(getEntityClass(), field.getName());
		String colName = field.getName();
//		if("id".equals(field.getName()))
//			System.out.println("id");
		int sqlType = getDialect().getTypeMapping().getType(field.getPropertyInfo().getType());
		ColumnInfo col = null;
		Column anno = field.getPropertyInfo().getAnnotation(Column.class);
		if(anno!=null){
			colName = anno.name();
			col = new ColumnInfo(tableInfo, colName, sqlType);
			col.setInsertable(anno.insertable());
			col.setUpdatable(anno.updatable());
		}else{
			colName = StringUtils.convert2UnderLineName(colName);
			col = new ColumnInfo(tableInfo, colName, sqlType);
		}
		col.setPrimaryKey(field.isIdentify());
		Basic basic = field.getPropertyInfo().getAnnotation(Basic.class);
		if(basic!=null){
			col.setFetchType(basic.fetch());
		}
		
		if(field.isIdentify()){
			col.setInsertable(!field.isIncreaseIdStrategy());
			col.setUpdatable(!field.isIncreaseIdStrategy());
		}
		
		return col;
	}
	
	@Override
	protected void buildIdGeneratorsOnClass(DbmMappedEntry entry){
		super.buildIdGeneratorsOnClass(entry);
		
		AnnotationInfo annotationInfo = entry.getAnnotationInfo();

		IdGeneratorFactory.createSequenceGenerator(annotationInfo)
							.ifPresent(idGenerator->entry.addIdGenerator(idGenerator));
		IdGeneratorFactory.createTableGenerator(annotationInfo)
							.ifPresent(idGenerator->entry.addIdGenerator(idGenerator));
	}
	
}
