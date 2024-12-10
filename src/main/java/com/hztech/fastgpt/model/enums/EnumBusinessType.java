package com.hztech.fastgpt.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.hztech.model.enums.IIntegerEnum;
import com.hztech.util.HzEnumUtils;

/**
 * EnumBusinessType
 *
 * @author: boboo
 * @Date: 2024/8/27 14:03
 **/
public enum EnumBusinessType implements IIntegerEnum {

    /**
     * "代表信息"
     */
    DEPUTY(1, "代表信息"),
    /**
     * "议案建议"
     */
    PROPOSAL(2, "议案建议"),
    /**
     * "履职活动"
     */
    ACTIVITY(3, "履职活动"),
    /**
     * "意见征集"
     */
    OPINION_COLLECTION(4, "意见征集"),
    /**
     * "新闻"
     */
    NEWS(5, "新闻"),
    /**
     * "民情直通"
     */
    PEOPLE_OPINION(6, "民情直通"),
    /**
     * "会议管理"
     */
    MEETING(7, "会议管理"),
    /**
     * "民生实事"
     */
    LIVELIHOOD_WORK(8, "民生实事"),
    /**
     * "监督工作"
     */
    SUPERVISE(9, "监督工作"),
    /**
     * 立法工作
     */
    LEGISLATION(10, "立法工作"),

    /**
     * 意见建议
     */
    SUGGESTION(11, "意见建议");

    /**
     * 值
     */
    private final Integer value;

    /**
     * 描述
     */
    private final String desc;

    EnumBusinessType(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    /**
     * 获取枚举值
     *
     * @return 枚举值
     */
    @Override
    @JsonValue
    public Integer getValue() {
        return this.value;
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

    @JsonCreator
    public static EnumBusinessType fromValue(Integer value) {
        return HzEnumUtils.fromValue(EnumBusinessType.class, value);
    }

    public static void main(String[] args) {
        StringBuffer stringBuffer = new StringBuffer();
        for (EnumBusinessType value : EnumBusinessType.values()) {
            stringBuffer.append(value.value + "：" + value.desc).append("\n");
        }
        System.out.println(stringBuffer);
    }
}
