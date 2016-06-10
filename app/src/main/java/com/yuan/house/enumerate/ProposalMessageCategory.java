package com.yuan.house.enumerate;

/**
 * Created by Alsor Zhou on 16/6/9.
 */
public enum ProposalMessageCategory {
    COMPLAINT(1), SUGGESTION(2), BUG(3);

    private int value;

    ProposalMessageCategory(int value) {
        this.value = value;
    }
}
