package com.hztech.fastgpt.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * ListCollectionResponseDTO
 *
 * @author: boboo
 * @Date: 2024/8/27 14:34
 **/
@Data
public class ListCollectionResponseDTO {

    private String _id;

    private String name;

    private LocalDateTime updateTime;
}
