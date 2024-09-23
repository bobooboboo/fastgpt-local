package com.hztech.fastgpt.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.hztech.model.enums.IIntegerEnum;
import com.hztech.util.HzEnumUtils;


/**
 * 时效性
 *
 * @author HZ
 */
public enum EnumLawStatus implements IIntegerEnum {

    /**
     * 尚未生效
     */
    NOT_EFFECTIVE(0, "尚未生效"),

    /**
     * 有效
     */
    EFFECTIVE(1, "有效"),

    /**
     * 已修改
     */
    MODIFIED(2, "已修改"),

    /**
     * 已废止
     */
    REPEALED(3, "已废止"),

    /**
     * 修改、废止的决定
     */
    NONE(4, "");

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
    EnumLawStatus(Integer value, String desc) {
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
    public static EnumLawStatus fromValue(Integer value) {
        return HzEnumUtils.fromValue(EnumLawStatus.class, value);
    }
}


