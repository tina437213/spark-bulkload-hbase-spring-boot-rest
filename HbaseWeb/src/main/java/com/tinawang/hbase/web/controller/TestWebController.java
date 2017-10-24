package com.tinawang.hbase.web.controller;

import com.tinawang.hbase.web.service.TestHbaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by Tina on 2017/8/7 0007.
 */
@Slf4j
@RestController
@RequestMapping("/testweb")
public class TestWebController {

    @Autowired
    private TestHbaseService testHbaseService;

    @GetMapping(value = "/list")
    public String getHbaseTableList(){
        log.info("================================================");
        log.info("begin process list request");
        String tableListStr;
        tableListStr = testHbaseService.getHbaseTableList();

        log.info("end process list request and return the result");
        log.info("================================================");
        return tableListStr;
    }

}
