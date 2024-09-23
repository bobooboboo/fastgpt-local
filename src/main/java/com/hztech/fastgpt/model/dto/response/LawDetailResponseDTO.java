package com.hztech.fastgpt.model.dto.response;

import com.hztech.fastgpt.model.enums.EnumLawContentType;
import lombok.Data;

/**
 * 法律法规数据对象
 *
 * @author: boboo
 * @Date: 2024/2/1 15:16
 **/
@Data
public class LawDetailResponseDTO {
    /**
     * 第几编
     */
    private Integer part;

    /**
     * 第几章
     */
    private Integer chapter;

    /**
     * 第几节
     */
    private Integer section;

    /**
     * 第几条
     */
    private Integer article;

    /**
     * 内容类型 EnumLawContentType [FOREWORD=0 序言; PART=1 编; CHAPTER=2 章; SECTION=3 节; ARTICLE=4 条]
     */
    private EnumLawContentType contentType;

    /**
     * 编章节条内容
     */
    private String content;
}
