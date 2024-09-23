package com.hztech.fastgpt.dao.impl;

import com.hztech.dao.base.HzFmBaseDao;
import com.hztech.fastgpt.dao.ILawDao;
import com.hztech.fastgpt.dao.mapper.LawMapper;
import com.hztech.fastgpt.dao.po.LawDO;
import com.hztech.fastgpt.dao.wrapper.LawQuery;
import com.hztech.fastgpt.dao.wrapper.LawUpdate;
import com.hztech.fastgpt.mapper.wrapper.LawMapperWrapper;
import org.springframework.stereotype.Repository;

/**
 * LawDaoImpl
 *
 * @author HZ
 */
@Repository
public class LawDaoImpl extends HzFmBaseDao<LawMapperWrapper, LawMapper, LawQuery, LawUpdate, LawDO, Long>
        implements ILawDao {

}