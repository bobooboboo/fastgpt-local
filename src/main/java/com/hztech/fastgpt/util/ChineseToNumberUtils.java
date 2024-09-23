package com.hztech.fastgpt.util;

import com.hztech.util.HzCollectionUtils;
import com.hztech.util.HzStringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 中文转数字工具类
 *
 * @author: boboo
 * @Date: 2023/11/8 10:10
 **/
public class ChineseToNumberUtils {

    /**
     * 创建对应的汉字
     */
    private static final ArrayList<String> CHINESE_NUMBER = HzCollectionUtils.newArrayList("零", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十", "百", "千", "万", "亿");

    /**
     * 创建与汉字对应的阿拉伯数字
     */
    private static final ArrayList<Integer> NUMBER = HzCollectionUtils.newArrayList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 100, 1000, 10000, 100000000);

    /**
     * 中文数字转换为阿拉伯数字
     *
     * @param str 需要转换的字符
     * @return 返回转换信息
     */
    public static Integer convertNumber(String str) {
        if (HzStringUtils.isBlank(str)) {
            return null;
        }
        //判断是否十纯中文汉字，不是则直接返回
        if (!isChineseNum(str)) {
            return null;
        }
        List<Integer> collect = str.chars()
                .mapToObj(c -> String.valueOf((char) c))
                .map(CHINESE_NUMBER::indexOf)
                .map(NUMBER::get)
                .collect(Collectors.toList());
        // 临时变量，存储最终的值
        int temp = 0;
        // 下标，进行到了哪里
        int index = 0;
        // 下一个数字是否是单位数字 大于等10则为单位
        boolean isTemp2 = false;
        for (Integer integer : collect) {
            // 如果下一个是单位，则此时不使用当前数字直接跳过
            if (isTemp2) {
                // 重置判断条件
                isTemp2 = false;
                // 下标正常增加
                index++;
                continue;
            }
            if (integer >= 10) {
                if (index != 0) {
                    // 此处修改于2023-08-09，csdn网友提出bug
                    if (temp != 0) {
                        temp = temp * integer;
                    } else {
                        temp = integer;
                    }
                } else {
                    temp = integer;
                }
            } else {
                // 如果下一个数字是单位，此时临时 变量直接 + 当前位数字 * 下一位单位
                if (index + 1 < collect.size() && collect.get(index + 1) >= 10) {
                    temp += integer * collect.get(index + 1);
                    isTemp2 = true;
                } else {
                    // 否则直接加当前数字即可
                    temp += integer;
                }
            }
            index++;
        }
        return temp;
    }

    /**
     * 判断传入的字符串是否全是汉字数字
     *
     * @param chineseStr 中文
     * @return 是否全是中文数字
     */
    private static boolean isChineseNum(String chineseStr) {
        char[] ch = chineseStr.toCharArray();
        for (char c : ch) {
            if (!CHINESE_NUMBER.contains(String.valueOf(c))) {
                return false;
            }
        }
        return true;
    }
}
