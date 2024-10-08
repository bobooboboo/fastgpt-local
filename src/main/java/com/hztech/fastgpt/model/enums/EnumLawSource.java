package com.hztech.fastgpt.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.hztech.model.enums.IIntegerEnum;
import com.hztech.util.HzEnumUtils;

/**
 * 数据来源
 *
 * @author: boboo
 * @Date: 2023/9/26 15:08
 **/
public enum EnumLawSource implements IIntegerEnum {

    /**
     * 未知
     */
    UNKNOWN(-1, "未知"),

    /**
     * 国家法律法规数据库
     */
    NATIONAL_LAWS_AND_REGULATIONS_DATABASE(0, "国家法律法规数据库"),

    /**
     * 国家规章库
     */
    NATIONAL_REGULATORY_REPOSITORY(1, "国家规章库"),

    /**
     * 杭州市地方性法规立项办法
     */
    MEASURES_FOR_THE_ESTABLISHMENT_OF_LOCAL_REGULATIONS_IN_HANGZHOU(2, "杭州市地方性法规立项办法");

    /**
     * 枚举值
     */
    private final Integer value;

    /**
     * 枚举描述
     */
    private final String desc;

    EnumLawSource(Integer value, String desc) {
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
    public static EnumLawSource fromValue(Integer value) {
        return HzEnumUtils.fromValue(EnumLawSource.class, value);
    }
}
