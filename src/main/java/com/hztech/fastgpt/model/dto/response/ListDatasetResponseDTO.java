package com.hztech.fastgpt.model.dto.response;

import lombok.Data;

/**
 * ListDatasetResponseDTO
 *
 * @author: boboo
 * @Date: 2024/8/28 16:55
 **/
@Data
public class ListDatasetResponseDTO {

    private String id;

    private String _id;

    private String name;

    private String parentId;

    public String getId() {
        return _id;
    }
}
