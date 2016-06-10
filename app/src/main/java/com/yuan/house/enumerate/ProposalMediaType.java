package com.yuan.house.enumerate;

/**
 * Created by Alsor Zhou on 16/6/9.
 */
public enum ProposalMediaType {
    TEXT(1), AUDIO(2), IMAGE(3);

    private int value;

    ProposalMediaType(int value) {
        this.value = value;
    }
}
