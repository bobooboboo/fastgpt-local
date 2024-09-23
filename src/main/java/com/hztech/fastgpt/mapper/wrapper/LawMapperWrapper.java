package com.hztech.fastgpt.mapper.wrapper;

import com.hztech.fastgpt.dao.mapper.LawMapper;
import com.hztech.fastgpt.dao.po.LawDO;
import com.hztech.fastgpt.dao.wrapper.LawQuery;
import com.hztech.fastgpt.dao.wrapper.LawUpdate;
import com.hztech.mapper.wrapper.BaseFmMapperWrapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * LawMapper包装器
 *
 * @author HZ
 */
@Component
public class LawMapperWrapper extends BaseFmMapperWrapper<LawMapper, LawQuery, LawUpdate, LawDO, Long> {

    public LawMapperWrapper(@Qualifier("lawMapper") LawMapper mapper) {
        super(mapper);
    }
}