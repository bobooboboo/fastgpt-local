package com.hztech.fastgpt.mapper.wrapper;

import com.hztech.fastgpt.dao.mapper.LawContentMapper;
import com.hztech.fastgpt.dao.po.LawContentDO;
import com.hztech.fastgpt.dao.wrapper.LawContentQuery;
import com.hztech.fastgpt.dao.wrapper.LawContentUpdate;
import com.hztech.mapper.wrapper.BaseFmMapperWrapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * LawContentMapper包装器
 *
 * @author HZ
 */
@Component
public class LawContentMapperWrapper extends BaseFmMapperWrapper<LawContentMapper, LawContentQuery, LawContentUpdate, LawContentDO, Long> {

    public LawContentMapperWrapper(@Qualifier("lawContentMapper") LawContentMapper mapper) {
        super(mapper);
    }
}