package com.lab.evaluation24;

import akka.actor.ActorRef;

public class ConfigMsg {

    private final ActorRef nextCLW;
    private final ActorRef nextCCLW;
    private final int W;
	private final int R;

    public ConfigMsg(ActorRef nextCLW, ActorRef nextCCLW, int W, int R) {
        this.nextCLW = nextCLW;
        this.nextCCLW = nextCCLW;
        this.W = W;
        this.R = R;
    }

    public ActorRef getNextCLW() {
        return this.nextCLW;
    }

    public ActorRef getNextCCLW() {
        return this.nextCCLW;
    }

    public int getW() {
        return this.W;
    }

    public int getR() {
        return this.R;
    }
}
