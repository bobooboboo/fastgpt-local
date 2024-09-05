package com.hztech.fastgpt.model.dto.request;

import com.hztech.fastgpt.entity.PushData;
import com.hztech.fastgpt.model.enums.EnumBusinessType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PushDataRequestDTO
 *
 * @author: boboo
 * @Date: 2024/8/27 11:42
 **/
@Data
public class PushDataRequestDTO {

    private EnumBusinessType type;

    private List<Data> data;


    @lombok.Data
    public static class Data {
        private String id;

        private LocalDateTime modifyTime;

        private List<PushData> data;
    }
}
