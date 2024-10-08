package com.hztech.fastgpt.dao.impl;

import com.hztech.dao.base.HzFmBaseDao;
import com.hztech.fastgpt.mapper.wrapper.HigherLevelLawMapperWrapper;
import com.hztech.fastgpt.dao.IHigherLevelLawDao;
import com.hztech.fastgpt.dao.mapper.HigherLevelLawMapper;
import com.hztech.fastgpt.dao.po.HigherLevelLawDO;
import com.hztech.fastgpt.dao.wrapper.HigherLevelLawQuery;
import com.hztech.fastgpt.dao.wrapper.HigherLevelLawUpdate;
import org.springframework.stereotype.Repository;

/**
 * HigherLevelLawDaoImpl
 *
 * @author HZ
 */
@Repository
public class HigherLevelLawDaoImpl extends HzFmBaseDao<HigherLevelLawMapperWrapper, HigherLevelLawMapper, HigherLevelLawQuery, HigherLevelLawUpdate, HigherLevelLawDO, Long>
        implements IHigherLevelLawDao {

}