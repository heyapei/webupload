package com.github.zyt.webuploader.mapper;

import com.github.zyt.webuploader.bean.Res;
import org.springframework.stereotype.Component;

/**
 * @Author 何亚培
 * @Version V1.0
 * @Date 2019/6/23 17:46
 * @Description: TODO
 */
@Component
public interface ResMapper {
    int deleteByPrimaryKey(Integer rId);

    int insert(Res record);

    int insertSelective(Res record);

    Res selectByPrimaryKey(Integer rId);

    Res selectByMD5(String rMD5);

    int updateByPrimaryKeySelective(Res record);

    int updateByPrimaryKey(Res record);
}