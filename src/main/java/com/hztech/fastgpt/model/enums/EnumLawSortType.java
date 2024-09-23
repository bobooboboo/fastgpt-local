package com.hztech.fastgpt.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.hztech.model.enums.IIntegerEnum;
import com.hztech.util.HzEnumUtils;

/**
 * 排序
 *
 * @author: boboo
 * @Date: 2023/9/26 15:08
 **/
public enum EnumLawSortType implements IIntegerEnum {

    /**
     * 相关度
     */
    RELEVANCE(0, "相关度"),

    /**
     * 发布时间升序
     */
    PUBLISH_ASC(1, "发布时间升序"),
    /**
     * 发布时间降序
     */
    PUBLISH_DESC(2, "发布时间降序"),
    /**
     * 实施时间升序
     */
    IMPLEMENTATION_ASC(3, "实施时间升序"),
    /**
     * 实施时间降序
     */
    IMPLEMENTATION_DESC(4, "实施时间降序");

    /**
     * 枚举值
     */
    private final Integer value;

    /**
     * 枚举描述
     */
    private final String desc;

    EnumLawSortType(Integer value, String desc) {
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
    public static EnumLawSortType fromValue(Integer value) {
        return HzEnumUtils.fromValue(EnumLawSortType.class, value);
    }
}
