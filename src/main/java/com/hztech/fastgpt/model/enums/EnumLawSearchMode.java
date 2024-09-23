package com.hztech.fastgpt.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.hztech.model.enums.IIntegerEnum;
import com.hztech.util.HzEnumUtils;

/**
 * 检索类型
 *
 * @author: boboo
 * @Date: 2023/9/26 15:08
 **/
public enum EnumLawSearchMode implements IIntegerEnum {

    /**
     * 智能
     */
    INTELLIGENT(0, "智能"),

    /**
     * 精确
     */
    ACCURATE(1, "精确");

    /**
     * 枚举值
     */
    private final Integer value;

    /**
     * 枚举描述
     */
    private final String desc;

    EnumLawSearchMode(Integer value, String desc) {
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
    public static EnumLawSearchMode fromValue(Integer value) {
        return HzEnumUtils.fromValue(EnumLawSearchMode.class, value);
    }
}
