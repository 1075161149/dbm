package org.onetwo.dbm.mapping;

import java.io.Serializable;
import java.util.Optional;

import javax.persistence.SequenceGenerator;
import javax.persistence.TableGenerator;

import org.onetwo.common.annotation.AnnotationInfo;
import org.onetwo.common.reflect.ReflectUtils;
import org.onetwo.common.spring.Springs;
import org.onetwo.dbm.annotation.DbmIdGenerator;
import org.onetwo.dbm.id.CustomIdGenerator;
import org.onetwo.dbm.id.CustomerIdGeneratorAdapter;
import org.onetwo.dbm.id.IdentifierGenerator;
import org.onetwo.dbm.id.SequenceGeneratorAttrs;
import org.onetwo.dbm.id.SequenceIdGenerator;
import org.onetwo.dbm.id.TableGeneratorAttrs;
import org.onetwo.dbm.id.TableIdGenerator;

/**
 * @author wayshall
 * <br/>
 */
public class IdGeneratorFactory {
	
	public static Optional<IdentifierGenerator<Long>> createSequenceGenerator(AnnotationInfo annotationInfo){
		SequenceGenerator sg = annotationInfo.getAnnotation(SequenceGenerator.class);
		if(sg==null){
			return Optional.empty();
		}
		SequenceGeneratorAttrs sgAttrs = new SequenceGeneratorAttrs(sg.name(), sg.sequenceName(), sg.initialValue(), sg.allocationSize());
		SequenceIdGenerator generator = new SequenceIdGenerator(sgAttrs);
		return Optional.of(generator);
	}
	
	public static Optional<IdentifierGenerator<Long>> createTableGenerator(AnnotationInfo annotationInfo){
		TableGenerator tg = annotationInfo.getAnnotation(TableGenerator.class);
		if(tg==null){
			return Optional.empty();
		}
		TableGeneratorAttrs tgAttrs = new TableGeneratorAttrs(tg.name(), 
																tg.allocationSize(), 
																tg.table(), 
																tg.pkColumnName(), 
																tg.valueColumnName(), 
																tg.pkColumnValue(),
																tg.initialValue());
		TableIdGenerator generator = new TableIdGenerator(tgAttrs);
		return Optional.of(generator);
	}
	
	@SuppressWarnings("unchecked")
	public static Optional<IdentifierGenerator<? extends Serializable>> createDbmIdGenerator(AnnotationInfo annotationInfo){
		DbmIdGenerator dg = annotationInfo.getAnnotation(DbmIdGenerator.class);
		if(dg==null){
			return Optional.empty();
		}
		CustomIdGenerator<? extends Serializable> customIdGenerator = Springs.getInstance().getBean(dg.generatorClass());
		if(customIdGenerator==null){
			customIdGenerator = ReflectUtils.newInstance(dg.generatorClass());
		}
		IdentifierGenerator<? extends Serializable> idGenerator = new CustomerIdGeneratorAdapter<>(dg.name(), customIdGenerator);
		return Optional.of(idGenerator);
	}

}