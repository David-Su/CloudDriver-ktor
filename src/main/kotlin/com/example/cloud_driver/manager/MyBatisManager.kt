package com.example.cloud_driver.manager

import org.apache.ibatis.io.Resources
import org.apache.ibatis.session.SqlSessionFactory
import org.apache.ibatis.session.SqlSessionFactoryBuilder

object MyBatisManager {
    val sessionFactory: SqlSessionFactory

    init {
        //加载mybatis核心配置文件
        val inputStream = Resources.getResourceAsStream("mybatis_config.xml")
        //获取SqlSessionFactory对象
        sessionFactory = SqlSessionFactoryBuilder().build(inputStream).also { factory ->
            //初始化所有表
            val session = factory.openSession()
            session.update("createUserTable")
            session.close()
        }
    }

}