package com.hztech.fastgpt.model.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.List;

/**
 * 已修订法规
 *
 * @author: boboo
 * @Date: 2024/10/23 10:26
 **/
@Data
public class QueryLawResponseDTO {

    /**
     * 总数量
     */
    private Integer total;

    /**
     * 法规信息
     */
    private List<QueryLawInfo> lawInfos;

    @Data
    public static class QueryLawInfo {
        @JsonIgnore
        private String outerId;

        /**
         * 法规标题
         */
        private String title;
        /**
         * 描述
         */
        private String lawInfo;
    }
}
