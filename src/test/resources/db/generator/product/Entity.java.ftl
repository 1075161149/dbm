<#assign requestPath="/${_globalConfig.getModuleName()}/${_tableContext.className}"/>
<#assign pagePath="/${_globalConfig.getModuleName()}/${_tableContext.tableNameWithoutPrefix}"/>

<#assign entityClassName="${_tableContext.className}Entity"/>
<#assign entityClassName2="${_tableContext.className}"/>
<#assign idName="${table.primaryKey.javaName}"/>
<#assign idType="${table.primaryKey.javaType.simpleName}"/>

package ${_tableContext.localFullPackage};

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.onetwo.dbm.jpa.BaseEntity;

import lombok.Data;

@SuppressWarnings("serial")
@Entity
@Table(name="${table.name}")
@Data
public class ${entityClassName2} extends BaseEntity  {

<#list table.columns as column>
    <#if column.primaryKey>
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    </#if>
    private ${column.javaType.simpleName} ${column.propertyName};
</#list>
}
