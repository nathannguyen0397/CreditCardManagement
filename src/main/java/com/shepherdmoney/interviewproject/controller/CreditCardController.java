package com.shepherdmoney.interviewproject.controller;

import com.shepherdmoney.interviewproject.model.BalanceHistory;
import com.shepherdmoney.interviewproject.model.CreditCard;
import com.shepherdmoney.interviewproject.model.User;
import com.shepherdmoney.interviewproject.repository.CreditCardRepository;
import com.shepherdmoney.interviewproject.repository.UserRepository;
import com.shepherdmoney.interviewproject.vo.request.AddCreditCardToUserPayload;
import com.shepherdmoney.interviewproject.vo.request.UpdateBalancePayload;
import com.shepherdmoney.interviewproject.vo.response.CreditCardView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@RestController
public class CreditCardController {

    // TODO: wire in CreditCard repository here (~1 line)
    @Autowired
    private CreditCardRepository creditCardRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/credit-card")
    public ResponseEntity<Integer> addCreditCardToUser(@RequestBody AddCreditCardToUserPayload payload) {
        // TODO: Create a credit card entity, and then associate that credit card with user with given userId
        //       Return 200 OK with the credit card id if the user exists and credit card is successfully associated with the user
        //       Return other appropriate response code for other exception cases
        //       Do not worry about validating the card number, assume card number could be any arbitrary format and length
        //return null;
        Optional<User> userOptional = userRepository.findById(payload.getUserId());

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            CreditCard creditCard = new CreditCard();
            creditCard.setIssuanceBank(payload.getCardIssuanceBank());
            creditCard.setNumber(payload.getCardNumber());
            creditCard.setOwner(user);
            creditCardRepository.save(creditCard);
            return ResponseEntity.ok(creditCard.getId());
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/credit-card:all")
    public ResponseEntity<List<CreditCardView>> getAllCardOfUser(@RequestParam int userId) {
        // TODO: return a list of all credit card associated with the given userId, using CreditCardView class
        //       if the user has no credit card, return empty list, never return null
        //return null;
        List<CreditCard> creditCards = creditCardRepository.findByOwnerId(userId);
        List<CreditCardView> creditCardViews = creditCards.stream().map(CreditCardView::fromCreditCard).collect(Collectors.toList());
        return ResponseEntity.ok(creditCardViews);
    }

    @GetMapping("/credit-card:user-id")
    public ResponseEntity<Integer> getUserIdForCreditCard(@RequestParam String creditCardNumber) {
        // TODO: Given a credit card number, efficiently find whether there is a user associated with the credit card
        //       If so, return the user id in a 200 OK response. If no such user exists, return 400 Bad Request
        //return null;
        Optional<CreditCard> optionalCreditCard = creditCardRepository.findByNumber(creditCardNumber);
        if (optionalCreditCard.isPresent()) {
            return ResponseEntity.ok(optionalCreditCard.get().getOwner().getId());
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/credit-card:update-balance")
    public ResponseEntity<Void> UpdateCreditCardBalanceHistory(@RequestBody UpdateBalancePayload[] payload) {
        //TODO: Given a list of transactions, update credit cards' balance history.
        //      For example: if today is 4/12, a credit card's balanceHistory is [{date: 4/12, balance: 110}, {date: 4/10, balance: 100}],
        //      Given a transaction of {date: 4/10, amount: 10}, the new balanceHistory is
        //      [{date: 4/12, balance: 120}, {date: 4/11, balance: 110}, {date: 4/10, balance: 110}]
        //      Return 200 OK if update is done and successful, 400 Bad Request if the given card number
        //        is not associated with a card.
        //return null;

        //iterate through the list
        //For every input transaction,
        //  check if there is a transaction in that date, update that transaction
        //  else if there is no transaction before that day, insert an entry

        for (UpdateBalancePayload p : payload) {
            Optional<CreditCard> optionalCard = creditCardRepository.findByNumber(p.getCreditCardNumber());
            //If there is the valid credit card
            if (optionalCard.isPresent()) {
                CreditCard card = optionalCard.get();
                Instant transactionTime = p.getTransactionTime();
                double transactionAmount = p.getTransactionAmount();

                List<BalanceHistory> balanceHistory = card.getBalanceHistory();

                // Check if a balance history record already exists for the transaction date
                Optional<BalanceHistory> optionalBalanceHistory = balanceHistory.stream()
                        .filter(bh -> bh.getDate().equals(transactionTime))
                        .findFirst();

                if (optionalBalanceHistory.isPresent()) {
                    // Update the existing balance history record
                    BalanceHistory existingBalanceHistory = optionalBalanceHistory.get();
                    double currentBalance = existingBalanceHistory.getBalance();
                    double newBalance = currentBalance + transactionAmount;
                    existingBalanceHistory.setBalance(newBalance);
                } else {
                    // Create a new balance history record for the transaction date
                    BalanceHistory newBalanceHistory = new BalanceHistory();
                    newBalanceHistory.setDate(transactionTime);
                    double currentBalance = balanceHistory.get(0).getBalance();
                    double newBalance = currentBalance + transactionAmount;
                    newBalanceHistory.setBalance(newBalance);
                    newBalanceHistory.setCreditCard(card);
                    balanceHistory.add(0, newBalanceHistory);
                }

                card.setBalanceHistory(balanceHistory);

                creditCardRepository.save(card);
            } else {
                // Return 400 Bad Request if the credit card number is not found
                return ResponseEntity.badRequest().build();
            }
        }

        // Return 200 OK if all updates are successful
        return ResponseEntity.ok().build();
    }
    
}
