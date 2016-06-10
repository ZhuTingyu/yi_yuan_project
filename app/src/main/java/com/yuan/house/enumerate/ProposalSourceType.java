package com.yuan.house.enumerate;

/**
 * Created by Alsor Zhou on 16/6/9.
 */
public enum ProposalSourceType {
    UNKNOWN(0), FROM_USER(1), FROM_AGENCY(2);

    private int value;

    ProposalSourceType(int value) {
        this.value = value;
    }
}
