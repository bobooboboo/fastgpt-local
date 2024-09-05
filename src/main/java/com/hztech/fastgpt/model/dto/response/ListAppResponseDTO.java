package com.hztech.fastgpt.model.dto.response;

import lombok.Data;

/**
 * ListAppResponseDTO
 *
 * @author: boboo
 * @Date: 2024/8/29 10:02
 **/
@Data
public class ListAppResponseDTO {

    private String id;
    private String _id;

    private String name;

    private String type;

    public String getId() {
        return _id;
    }
}
