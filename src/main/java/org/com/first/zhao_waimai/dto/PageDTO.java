package org.com.first.zhao_waimai.dto;

import lombok.Data;

import java.util.List;

/**
 * 通用分页响应
 */
@Data
public class PageDTO<T> {

    /** 当前页数据 */
    private List<T> list;

    /** 总条数 */
    private Long total;

    /** 页码 */
    private Long page;

    /** 每页大小 */
    private Long size;
}
