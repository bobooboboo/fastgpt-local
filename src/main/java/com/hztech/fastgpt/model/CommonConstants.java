package com.hztech.fastgpt.model;

import java.util.regex.Pattern;

/**
 * CommonConstants
 *
 * @author: boboo
 * @Date: 2024/10/17 17:44
 **/
public interface CommonConstants {

    Pattern JSON_ARRAY_PATTERN = Pattern.compile("\\s*\\[\\s*(?:[^\\[\\]]*)*\\s*]");
}
