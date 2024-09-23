package com.hztech.fastgpt.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.hztech.model.enums.IIntegerEnum;
import com.hztech.util.HzEnumUtils;

/**
 * EnumLegislationMode
 *
 * @author: boboo
 * @Date: 2023/10/24 16:37
 **/
public enum EnumLawMode implements IIntegerEnum {
    /**
     * 智能
     */
    FULL(1, "全文检索"),

    /**
     * 精确
     */
    STRIP(2, "条文检索");

    /**
     * 枚举值
     */
    private final Integer value;

    /**
     * 枚举描述
     */
    private final String desc;

    EnumLawMode(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @Override
    public String getDesc() {
        return desc;
    }

    @Override
    @JsonValue
    public Integer getValue() {
        return value;
    }

    /**
     * 从json反序列时设值
     */
    @JsonCreator
    public static EnumLawMode fromValue(Integer value) {
        return HzEnumUtils.fromValue(EnumLawMode.class, value);
    }
}
