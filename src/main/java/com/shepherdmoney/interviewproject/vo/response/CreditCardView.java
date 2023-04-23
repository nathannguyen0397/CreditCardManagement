package com.shepherdmoney.interviewproject.vo.response;

import com.shepherdmoney.interviewproject.model.CreditCard;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class CreditCardView {

    private String issuanceBank;

    private String number;

    public static CreditCardView fromCreditCard(CreditCard creditCard) {
        return CreditCardView.builder()
                .issuanceBank(creditCard.getIssuanceBank())
                .number(creditCard.getNumber())
                .build();
    }

}
