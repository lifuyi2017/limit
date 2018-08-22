package com.cigc.limit.service;

import java.util.List;

/**
 * Created by Administrator on 2018/6/29 0029.
 *套牌车类
 */
public class TaoPaiEntity {

    private String codeColor;
    //异常数
    private Integer exception1;
    //异常车牌(抓拍)
    private String codeException;
    //异常且车牌号相同的数
    private Integer exception2;
    //比例
    private double bili;

    public List<String> getCodeList() {
        return codeList;
    }

    public void setCodeList(List<String> codeList) {
        this.codeList = codeList;
    }

    //中间集合
    private List<String> codeList;

    public double getBili() {
        return bili;
    }

    public void setBili(double bili) {
        this.bili = bili;
    }

    public TaoPaiEntity() {

    }

    public String getCodeColor() {
        return codeColor;
    }

    public void setCodeColor(String codeColor) {
        this.codeColor = codeColor;
    }

    public Integer getException1() {
        return exception1;
    }

    public void setException1(Integer exception1) {
        this.exception1 = exception1;
    }

    public String getCodeException() {
        return codeException;
    }

    public void setCodeException(String codeException) {
        this.codeException = codeException;
    }

    public Integer getException2() {
        return exception2;
    }

    public void setException2(Integer exception2) {
        this.exception2 = exception2;
    }
}
