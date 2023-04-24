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

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
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
            creditCard.setIssuanceBank(payload.getIssuanceBank());
            creditCard.setNumber(payload.getNumber());
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

    //TODO: Given a list of transactions, update credit cards' balance history.
    //      For example: if today is 4/12, a credit card's balanceHistory is [{date: 4/12, balance: 110}, {date: 4/10, balance: 100}],
    //      Given a transaction of {date: 4/10, amount: 10}, the new balanceHistory is
    //      [{date: 4/12, balance: 120}, {date: 4/11, balance: 110}, {date: 4/10, balance: 110}]
    //      Return 200 OK if update is done and successful, 400 Bad Request if the given card number
    //        is not associated with a card.
    //return null;

    @PostMapping("/credit-card:update-balance")
    public ResponseEntity<Void> UpdateCreditCardBalanceHistory(@RequestBody UpdateBalancePayload[] payload) {
        for (UpdateBalancePayload p : payload) {
            Optional<CreditCard> optionalCard = creditCardRepository.findByNumber(p.getNumber());
            if (optionalCard.isPresent()) {
                CreditCard card = optionalCard.get();
                Instant transactionDate = p.getDate();
                double transactionAmount = p.getBalance();

                List<BalanceHistory> balanceHistory = card.getBalanceHistory();
                Collections.reverse(balanceHistory); // Reverse the list

                int insertIndex = 0;
                double previousBalance = 0.0;
                boolean updateExisting = false;

                for (int i = 0; i < balanceHistory.size(); i++) {
                    if (balanceHistory.get(i).getDate().isAfter(transactionDate)) {
                        insertIndex = i;
                        if (i > 0) {
                            previousBalance = balanceHistory.get(i - 1).getBalance();
                            if (balanceHistory.get(i - 1).getDate().equals(transactionDate)) {
                                // Update the existing balance history record
                                double currentBalance = balanceHistory.get(i - 1).getBalance();
                                double newBalance = currentBalance + transactionAmount;
                                balanceHistory.get(i - 1).setBalance(newBalance);
                                updateExisting = true;
                            }
                        }
                        break;
                    }
                }

                if (!updateExisting) {
                    // Create a new balance history entry with the updated balance
                    BalanceHistory newBalanceHistory = new BalanceHistory();
                    newBalanceHistory.setDate(transactionDate);
                    newBalanceHistory.setBalance(previousBalance + transactionAmount);
                    newBalanceHistory.setCreditCard(card);

                    // Insert the new entry of current balance at the correct position
                    balanceHistory.add(insertIndex, newBalanceHistory);

                    // Update the gap entries and the pivot entry
                    int pivotIndex = insertIndex + 1;
                    Instant currentDate = transactionDate.plus(Duration.ofDays(1));

                    if (pivotIndex < balanceHistory.size()) {
                        while (currentDate.isBefore(balanceHistory.get(pivotIndex).getDate())) {
                            BalanceHistory gapEntry = new BalanceHistory();
                            gapEntry.setCreditCard(card);
                            gapEntry.setDate(currentDate);
                            gapEntry.setBalance(newBalanceHistory.getBalance());
                            balanceHistory.add(pivotIndex, gapEntry);

                            currentDate = currentDate.plus(Duration.ofDays(1));
                            pivotIndex++;
                        }

                        // Update the pivot entry (the entry immediately after the gap)
                        balanceHistory.get(pivotIndex).setBalance(balanceHistory.get(pivotIndex).getBalance() + transactionAmount);
                    }
                }

                Collections.reverse(balanceHistory); // Reverse the list back to its original order
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



    public void printBalanceHistoryTable(List<BalanceHistory> balanceHistory) {
        if (balanceHistory.isEmpty()) {
            System.out.println("No balance history found.");
            return;
        }

        String tableFormat = "| %-6s | %-10s | %-25s |%n";
        System.out.format("| ID     | Balance    | Date                     |%n");
        System.out.format("-------------------------------------------------+%n");

        for (BalanceHistory bh : balanceHistory) {
            System.out.format(tableFormat, bh.getId(), String.format("%.2f", bh.getBalance()), bh.getDate());
        }
    }
    
}

   /* TRUNCATE TABLE BALANCE_HISTORY;

INSERT INTO balance_history (id, credit_card_id, date, balance) VALUES (1, 1, '2023-04-09T00:00:00.000Z', 0);
INSERT INTO balance_history (id, credit_card_id, date, balance) VALUES (2, 1, '2023-04-12T00:00:00.000Z', 9990);
INSERT INTO balance_history (id, credit_card_id, date, balance) VALUES (3, 1 , '2023-04-17T00:00:00.000Z', 0);

SELECT * FROM CREDIT_CARD ;
SELECT * FROM BALANCE_HISTORY  ;

SELECT * FROM MY_USER   ;
  */