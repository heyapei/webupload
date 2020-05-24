package com.github.zyt.webuploader.mapper;

import com.github.zyt.webuploader.bean.Res;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;


/**
 * @Author 何亚培
 * @Version V1.0
 * @Date 2020/5/24 19:06
 * @Description: TODO
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class ResMapperTest {


    @Autowired
    private ResMapper resMapper;

    @Test
    public void showOneArticle() {
       Res videoImgDOList = resMapper.selectByMD5("ce890bdd319878e3b7eab190ceac26a3");
        System.out.println(videoImgDOList);
    }
}