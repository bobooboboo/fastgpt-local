package com.hztech.fastgpt.model.dto.request;

import com.hztech.fastgpt.model.enums.EnumLawStatus;
import lombok.Data;

/**
 * 法规查询参数
 *
 * @author: boboo
 * @Date: 2024/10/23 9:52
 **/
@Data
public class QueryLawRequestDTO {
    /**
     * 检索关键字，这里包括城市、制定机关
     */
    private String keyword;

    /**
     * 法规时效性
     */
    private EnumLawStatus status;

    /**
     * 年份
     */
    private Integer year;
}
