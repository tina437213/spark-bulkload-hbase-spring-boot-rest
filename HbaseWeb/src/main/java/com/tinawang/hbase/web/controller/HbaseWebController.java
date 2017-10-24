package com.tinawang.hbase.web.controller;

import com.tinawang.hbase.web.domain.request.RequestCheck;
import com.tinawang.hbase.web.domain.request.RequestOpinion;
import com.tinawang.hbase.web.domain.response.Response;
import com.tinawang.hbase.web.service.CheckQueryService;
import com.tinawang.hbase.web.service.OpinionQueryService;
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
@RequestMapping("/hbaseweb")
public class HbaseWebController {

    @Autowired
    private OpinionQueryService opinionQueryService;

    @Autowired
    private CheckQueryService checkQueryService;

    @GetMapping(value = "/get/opinion")
    public Response findByBussNoAndBussType(RequestOpinion requestOpinion){
        String bussNo = requestOpinion.getBuss_no();
        String bussType = requestOpinion.getBusiness_type();
        return opinionQueryService.findByBussNoAndBussType(bussNo,bussType);

    }

    @GetMapping(value = "/get/check")
    public Response findByCheckId(RequestCheck requestCheck){
        String checkId = requestCheck.getCheck_id();
        return checkQueryService.findByCheckId(checkId);

    }





}
