package com.shepherdmoney.interviewproject.vo.request;

import lombok.Data;

import java.time.Instant;

@Data
public class UpdateBalancePayload {

    private String number;
    
    private Instant date;

    private double balance;
}
