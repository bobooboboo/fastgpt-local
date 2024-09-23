package com.hztech.fastgpt.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.hztech.model.enums.IIntegerEnum;
import com.hztech.util.HzEnumUtils;

/**
 * 检索范围
 *
 * @author: boboo
 * @Date: 2023/9/27 14:25
 **/
public enum EnumLawSearchType implements IIntegerEnum {
    /**
     * 全文
     */
    FULL(0, "全文"),
    /**
     * 标题
     */
    TITLE(1, "标题"),
    /**
     * 内容
     */
    CONTENT(2, "内容"),
    ;

    /**
     * 枚举值
     */
    private final Integer value;

    /**
     * 枚举描述
     */
    private final String desc;

    EnumLawSearchType(Integer value, String desc) {
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
    public static EnumLawSearchType fromValue(Integer value) {
        return HzEnumUtils.fromValue(EnumLawSearchType.class, value);
    }
}
