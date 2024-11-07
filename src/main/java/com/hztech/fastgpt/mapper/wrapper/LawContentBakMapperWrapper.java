package com.hztech.fastgpt.mapper.wrapper;

import com.hztech.fastgpt.dao.mapper.LawContentBakMapper;
import com.hztech.fastgpt.dao.po.LawContentBakDO;
import com.hztech.fastgpt.dao.wrapper.LawContentBakQuery;
import com.hztech.fastgpt.dao.wrapper.LawContentBakUpdate;
import com.hztech.mapper.wrapper.BaseFmMapperWrapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * LawContentBakMapper包装器
 *
 * @author HZ
 */
@Component
public class LawContentBakMapperWrapper extends BaseFmMapperWrapper<LawContentBakMapper, LawContentBakQuery, LawContentBakUpdate, LawContentBakDO, Long> {

    public LawContentBakMapperWrapper(@Qualifier("lawContentBakMapper") LawContentBakMapper mapper) {
        super(mapper);
    }
}