package com.hztech.fastgpt.mapper.wrapper;

import com.hztech.fastgpt.dao.mapper.HigherLevelLawMapper;
import com.hztech.fastgpt.dao.po.HigherLevelLawDO;
import com.hztech.fastgpt.dao.wrapper.HigherLevelLawQuery;
import com.hztech.fastgpt.dao.wrapper.HigherLevelLawUpdate;

import com.hztech.mapper.wrapper.BaseFmMapperWrapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * HigherLevelLawMapper包装器
 *
 * @author HZ
 */
@Component
public class HigherLevelLawMapperWrapper extends BaseFmMapperWrapper<HigherLevelLawMapper, HigherLevelLawQuery, HigherLevelLawUpdate, HigherLevelLawDO, Long> {

    public HigherLevelLawMapperWrapper(@Qualifier("higherLevelLawMapper") HigherLevelLawMapper mapper) {
        super(mapper);
    }
}