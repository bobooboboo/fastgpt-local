package com.hztech.fastgpt.model.dto.request;

import lombok.Data;

/**
 * SaveSenceDataRequestDTO
 *
 * @author: boboo
 * @Date: 2024/9/24 11:24
 **/
@Data
public class SaveSceneDataRequestDTO {

    /**
     * 对话id
     */
    private String chatId;

    /**
     * 接口数据
     */
    private Object data;

    /**
     * 场景code
     */
    private String code;

    private Object userSelectData;
}
