package com.lab.evaluation24;

import com.addressBook.AddressBookClientActor;

import akka.actor.AbstractActorWithStash;
import akka.actor.ActorRef;
import akka.actor.Props;

public class BallPasserActor extends AbstractActorWithStash {

    private ActorRef nextCLW;
    private ActorRef nextCCLW;
    private int W;
    private int R;
    private int ballsPassed;
    private int ballsHold;

    public BallPasserActor() {
        this.ballsPassed = 0;
        this.ballsHold = 0;
    }
    
    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(ConfigMsg.class, this::configure)
            .match(BallMsg.class, this::handleBall)
            .build();
    }

    private void configure(ConfigMsg msg) {
        System.out.println("Received configuration message! " + self());
        nextCLW = msg.getNextCLW();
        nextCCLW = msg.getNextCCLW();
        W = msg.getW();
        R = msg.getR();
    }

    private void handleBall(BallMsg msg) {
		// if (sender() == ActorRef.noSender()) {
		// 	// Message coming from outside the actor system
		// 	System.out.println("CLIENT: Starts passing ball");
		// 	msg.setSender(self());
		// 	server.tell(msg, self());
	 	// } else {
		// 	// The server is sending this back 
	 	// 	System.out.println("CLIENT: Received reply, text: " + msg.getText());
	 	// }

        //Check if it is in rest mode
        if (ballsPassed == W) {
            stash();
            ballsHold++;
            System.out.println("Hold ball number " + ballsHold + " by " + self());
            msg.setAlreadyHeld();

            if (ballsHold == R){
                System.out.println("Resume " + self());
                ballsPassed = 0;
                unstashAll();
            }
        } else { //active mode
            if (self() == sender()) { //it is the initial sender
                System.out.println("Ball dropped by " + self());
            } else {
                if (msg.getInitialSender() == ActorRef.noSender()) { //If the sender is not an actor but the main function
                    System.out.println("Ball received by main function " + self());
                    msg.setInitialSender(self());
                }else if (!msg.getAlreadyHeld()) {
                    ballsPassed++;
                    System.out.println("The ball has not been already held");
                }
                //tell method to implement
                if (msg.getDirection() == BallMsg.COUNTERCLOCKWISE) {
                nextCCLW.tell(msg, msg.getInitialSender());
                } else {
                    nextCLW.tell(msg, msg.getInitialSender());
                }
                System.out.println("Passed ball" + self());
            }
        }   
    }

    static Props props() {
		return Props.create(BallPasserActor.class);
	}
}
