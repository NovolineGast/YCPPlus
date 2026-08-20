package com.ycpplus.admin.dto;

public class GenerateKeysRequest {
    private int amount;
    private int days;
    private String prefix;

    public GenerateKeysRequest() {}

    public GenerateKeysRequest(int amount, int days, String prefix) {
        this.amount = amount;
        this.days = days;
        this.prefix = prefix;
    }

    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }

    public int getDays() { return days; }
    public void setDays(int days) { this.days = days; }

    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }
}
