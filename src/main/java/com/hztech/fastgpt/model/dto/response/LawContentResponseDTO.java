package com.hztech.fastgpt.model.dto.response;

import lombok.Data;

/**
 * LawContentResponseDTO
 *
 * @author: boboo
 * @Date: 2024/9/23 20:32
 **/
@Data
public class LawContentResponseDTO {

    /**
     * 外部法规id
     */
    private String outerId;

    /**
     * 法规内容
     */
    private String content;
}
