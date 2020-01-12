<#assign apiName="${table.propertyName}Api"/>
<#assign formComponentName="${table.propertyName}Form"/>
<#assign moduleName="${_globalConfig.getModuleName()}"/>
<template>
  <div class="app-container">

    <layout-table
      ref="listTable"
      id-property="${table.primaryKey.javaName}"
      :list-api="listApi"
      :query-form-model="queryFormModel"
      :refresh.sync="refreshTable"
      :delete-api="deleteApi"
      :operations="operations">
      <template slot="queryForm">
  <#list table.columns as column>
    <#if !column.primaryKey>
        <el-form-item label="${(column.comments[0])!''}">
          <el-input v-model="queryFormModel.${column.javaName}" placeholder="${(column.comments[0])!''}"/>
        </el-form-item>
    </#if>
  </#list>
      </template>

      <template slot="toolbar">
        <el-button type="primary" icon="el-icon-edit" @click="handleAdd">
          添加${(table.comments[0])!''}
        </el-button>
      </template>

      <el-table-column align="center" width="80" type="selection"/>
  <#list UIClassMeta.listableFields as field>
    <#if field.column.isDateType()>
      <el-table-column align="center" label="${(field.label)!''}" <#if field?counter != UIClassMeta.listableFields.size()>width="100"</#if>>
        <template slot-scope="scope">
          <span>{{ scope.row.${field.column.javaName} | formatDateInMillis }}</span>
        </template>
      </el-table-column>
    <#else>
      <el-table-column align="center" label="${(field.label)!''}" prop="${field.column.javaName}" <#if field?counter != UIClassMeta.listableFields.size()>width="100"</#if>/>
    </#if>
  </#list>
    </layout-table>

    <el-dialog title="${UIClassMeta.label}管理"
      :visible.sync="dialog.visible"
      :close-on-click-modal="false"
      :before-close="handleClose">
      <el-tabs type="border-card">
        <el-tab-pane label="${UIClassMeta.label}编辑">
          <${table.propertyName}-form :status-mode="dialog.status" :data-model="dataModel" @finishHandle="on${_tableContext.className}Finish"/>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script>
import layoutTable from '@/components/xui/layoutTable'
import * as ${apiName} from '@/api/${vueModuleName}/${apiName}'
import ${formComponentName} from './${formComponentName}'

export default {
  name: '${_tableContext.className}',
  components: {
    ${formComponentName},
    layoutTable
  },
  data() {
    return {
      queryFormModel: {
  <#list UIClassMeta.formFields as field>
    <#if !field.column.primaryKey>
        ${field.column.javaName}: '',
    </#if>
  </#list>
        ${table.primaryKey.javaName}: null
      },
      dialog: {
        status: '',
        visible: false
      },
      dataModel: this.initDataModel(),
      refreshTable: false,
      operations: [
        { action: 'edit', text: '编辑', handler: this.handleEdit }
      ]
    }
  },
  mounted: function() {
  },
  methods: {
    handleClose() {
      // 清除验证信息
      // this.$refs.dataForm.resetFields()
      this.dialog.visible = false
      return true
    },
    on${_tableContext.className}Finish() {
      this.refreshTable = true
      this.dialog.visible = false
    },
    listApi: ${apiName}.getList,
    deleteApi: ${apiName}.remove,
    // 初始化dataModel
    initDataModel() {
      return {
  <#list UIClassMeta.formFields as field>
    <#if !field.column.primaryKey>
      <#if field.column.isFileType()>
        ${field.column.javaName}File: null,
      <#else>
        ${field.column.javaName}: '',
      </#if>
    </#if>
  </#list>
        ${table.primaryKey.javaName}: null
      }
    },
    // 操作菜单处理，根据command分派到不同方法
    handleAction(data) {
      const command = data.action
      if (command === 'edit') {
        this.handleEdit(data.row)
      } else if (command === 'delete') {
        this.$refs.listTable.handleDelete([data.row.${table.primaryKey.javaName}])
      }
    },
    handleAdd() {
      this.dialog.status = 'Add'
      this.dialog.visible = true
      this.dataModel = this.initDataModel()
    },
    handleEdit(row) {
      this.dialog.status = 'Edit'
      this.dialog.visible = true
      this.dialog.dataId = row.${table.primaryKey.javaName}
      ${apiName}.get(row.${table.primaryKey.javaName}).then(res => {
        this.dataModel = res.data.data
      })
    }
  }
}
</script>

<style lang="scss">
.text-wrapper {
  white-space: pre-wrap;
}
</style>

