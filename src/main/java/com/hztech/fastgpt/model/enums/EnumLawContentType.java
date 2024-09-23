package com.hztech.fastgpt.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.hztech.model.enums.IIntegerEnum;
import com.hztech.util.HzEnumUtils;


/**
 * 内容类型
 *
 * @author HZ
 */
public enum EnumLawContentType implements IIntegerEnum {

    /**
     * 序言
     */
    FOREWORD(0, "序言"),

    /**
     * 编
     */
    PART(1, "编"),

    /**
     * 章
     */
    CHAPTER(2, "章"),

    /**
     * 节
     */
    SECTION(3, "节"),

    /**
     * 条
     */
    ARTICLE(4, "条"),

    /**
     * 空行
     */
    EMPTY_LINE(5, "空行"),

    /**
     * 不在目录中展示的其他非空行类别，比如题注、目录等
     */
    OTHER(6, "其他");

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
    EnumLawContentType(Integer value, String desc) {
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
    public static EnumLawContentType fromValue(Integer value) {
        return HzEnumUtils.fromValue(EnumLawContentType.class, value);
    }
}


