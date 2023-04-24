package com.shepherdmoney.interviewproject.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class CreditCard {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    private String issuanceBank;

    private String number;

    // TODO: Credit card's owner. For detailed hint, please see User class
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User owner;



    // TODO: Credit card's balance history. It is a requirement that the dates in the balanceHistory
    //       list must be in chronological order, with the most recent date appearing first in the list. 
    //       Additionally, the first object in the list must have a date value that matches today's date, 
    //       since it represents the current balance of the credit card. For example:
    //       [
    //         {date: '2023-04-13', balance: 1500},
    //         {date: '2023-04-12', balance: 1200},
    //         {date: '2023-04-11', balance: 1000},
    //         {date: '2023-04-10', balance: 800}
    //       ]
    @OneToMany(mappedBy = "creditCard", cascade = CascadeType.ALL)
    @OrderBy("date DESC")
    private List<BalanceHistory> balanceHistory;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getIssuanceBank() {
        return issuanceBank;
    }

    public void setIssuanceBank(String issuanceBank) {
        this.issuanceBank = issuanceBank;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public List<BalanceHistory> getBalanceHistory() {
        Instant today = Instant.now().truncatedTo(ChronoUnit.DAYS);

        if (balanceHistory == null) {
            balanceHistory = new ArrayList<>();
            BalanceHistory newEntry = new BalanceHistory();
            newEntry.setDate(today);
            newEntry.setBalance(0.0);
            newEntry.setCreditCard(this);
            balanceHistory.add(newEntry);
        } else {
            // Sort the list in descending order by date
            Collections.sort(balanceHistory, new Comparator<BalanceHistory>() {
                @Override
                public int compare(BalanceHistory bh1, BalanceHistory bh2) {
                    return bh2.getDate().compareTo(bh1.getDate());
                }
            });
            Instant mostRecentDate = balanceHistory.get(0).getDate().truncatedTo(ChronoUnit.DAYS);
            if (!mostRecentDate.equals(today)) {
                BalanceHistory newEntry = new BalanceHistory();
                newEntry.setDate(today);
                newEntry.setBalance(balanceHistory.get(0).getBalance());
                newEntry.setCreditCard(this);
                balanceHistory.add(0, newEntry);
            }
        }

        return balanceHistory;
    }
//    public List<BalanceHistory> getBalanceHistory() {
//        LocalDate today = LocalDate.now();
//
//        if (balanceHistory == null) {
//            balanceHistory = new ArrayList<>();
//            BalanceHistory newEntry = new BalanceHistory();
//            newEntry.setDate(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
//            newEntry.setBalance(0.0);
//            newEntry.setCreditCard(this);
//            balanceHistory.add(newEntry);
//        } else {
//            // Sort the list in descending order by date
//            Collections.sort(balanceHistory, new Comparator<BalanceHistory>() {
//                @Override
//                public int compare(BalanceHistory bh1, BalanceHistory bh2) {
//                    return bh2.getDate().compareTo(bh1.getDate());
//                }
//            });
//            LocalDate mostRecentDate = balanceHistory.get(0).getDate().atZone(ZoneId.systemDefault()).toLocalDate();
//            if (!mostRecentDate.isEqual(today)) {
//                BalanceHistory newEntry = new BalanceHistory();
//                newEntry.setDate(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
//                newEntry.setBalance(balanceHistory.get(0).getBalance());
//                newEntry.setCreditCard(this);
//                balanceHistory.add(0, newEntry);
//            }
//        }
//
//        return balanceHistory;
//    }

    public void setBalanceHistory(List<BalanceHistory> balanceHistory) {
        this.balanceHistory = balanceHistory;
    }
}
