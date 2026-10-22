package org.com.first.zhao_waimai.service;

import org.com.first.zhao_waimai.dto.AdminMallSaveDTO;
import org.com.first.zhao_waimai.entity.Mall;

import java.util.List;

/**
 * 平台端美食城管理
 */
public interface AdminMallService {

    /** 美食城列表（含关闭状态） */
    List<Mall> list();

    /** 新增/编辑，返回 id */
    Long save(Long mallId, AdminMallSaveDTO dto);

    /** 删除（存在档口时禁止） */
    void delete(Long mallId);
}
