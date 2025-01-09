package com.lab.evaluation24;

import akka.actor.ActorRef;

public class BallMsg {
	
	public static final int COUNTERCLOCKWISE = 0;
	public static final int CLOCKWISE = 1;

    private final int direction;
    private boolean alreadyHeld = false;
    private ActorRef initialSender;

    public BallMsg(int direction, ActorRef initialSender) {
        this.direction = direction;
        this.initialSender = initialSender;
    }

    public int getDirection() {
        return this.direction;
    }

    public boolean getAlreadyHeld() {
        return this.alreadyHeld;
    }

    public void setAlreadyHeld() {
        this.alreadyHeld = true;
    }

    public ActorRef getInitialSender() {
        return this.initialSender;
    }
    
    public void setInitialSender(ActorRef initialSender) {
        this.initialSender = initialSender;
    }
}
