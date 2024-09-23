package com.hztech.fastgpt.model.dto.response;

import com.hztech.fastgpt.model.enums.EnumLawType;
import lombok.Data;

/**
 * 国家法律法规数据库列表页请求响应对象
 *
 * @author: boboo
 * @Date: 2023/9/26 13:56
 **/
@Data
public class CountryLawBasicDataResponseDTO {

    /**
     * id
     */
    private String id;
    /**
     * 制定机关
     */
    private String office;
    /**
     * 生效日期
     */
    private String expiry;
    /**
     * 公布日期
     */
    private String publish;
    /**
     * 时效性
     */
    private String status;
    /**
     * 标题
     */
    private String title;

    private String type;
    /**
     * 法律性质
     */
    private EnumLawType legislationType;
}
