package com.hztech.fastgpt.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.hztech.model.enums.IIntegerEnum;
import com.hztech.util.HzEnumUtils;


/**
 * 法律性质
 *
 * @author HZ
 */
public enum EnumLawType implements IIntegerEnum {
    NONE(-1, "未知"),
    /**
     * 宪法
     */
    CONSTITUTION(0, "宪法"),

    /**
     * 法律
     */
    STATUTE(1, "法律"),

    /**
     * 行政法规
     */
    ADMINISTRATIVE_REGULATIONS(2, "行政法规"),

    /**
     * 监察法规
     */
    SUPERVISION_REGULATIONS(3, "监察法规"),

    /**
     * 司法解释
     */
    JUDICIAL_INTERPRETATION(4, "司法解释"),

    /**
     * 地方性法规
     */
    LOCAL_REGULATIONS(5, "地方性法规"),

    LEGAL_INTERPRETATION(6, "法律解释"),

    LEGAL_QUESTIONS(7, "有关法律问题和重大问题的决定"),

    MODIFICATION_AND_ABOLITION(8, "修改、废止的决定"),
    ;

    /**
     * 枚举值
     */
    private final Integer value;

    /**
     * 枚举描述
     */
    private final String desc;

    /**
     * 构造函数
     */
    EnumLawType(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    /**
     * 获取枚举描述
     *
     * @return 枚举描述
     */
    @Override
    public String getDesc() {
        return this.desc;
    }

    /**
     * 枚举数据库存储值、序列化json时取值
     */
    @Override
    @JsonValue
    public Integer getValue() {
        return this.value;
    }

    /**
     * 从json反序列时设值
     */
    @JsonCreator
    public static EnumLawType fromValue(Integer value) {
        return HzEnumUtils.fromValue(EnumLawType.class, value);
    }
}


