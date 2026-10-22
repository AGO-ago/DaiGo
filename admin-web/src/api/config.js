import request from './request'

/** 全部配置项 */
export function listConfigs() {
  return request.get('/admin/configs')
}

/** 批量保存配置（键值对） */
export function updateConfigs(values) {
  return request.put('/admin/configs', values)
}
