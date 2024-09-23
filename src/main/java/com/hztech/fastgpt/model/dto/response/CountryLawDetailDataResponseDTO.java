package com.hztech.fastgpt.model.dto.response;

import lombok.Data;

/**
 * 国家法律法规数据库详情页请求响应对象
 *
 * @author: boboo
 * @Date: 2023/9/26 14:03
 **/
@Data
public class CountryLawDetailDataResponseDTO {
    /**
     * 文件类型 WORD、PDF、HTML
     */
    private String type;
    /**
     * 文件路径
     */
    private String path;

    /**
     * HTML地址
     */
    private String url;
}
