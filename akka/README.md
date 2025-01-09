Evaluation lab - Akka

## Group number: 26

## Group members

- Armando Fiorini
- Christian Mariano
- MohammadAmin Rahimi

## Description of message flows
 - At the main function, first we configure the system with the details written on "ConfigMsg" class.
 - Then we create neighbours connection between nodes with clockWise(CLW) and counterClockWise(CCLW) arrangements. (ClockWise: A>B>C>D)d
 - The message contains the sender and the direction as attributes.
 - When an actor receives a message from Main it sets itself as the sender of the message.
 - When an actor receives a message while he is resting it increments the held messages counter and holds the message.
 - When that counter goes to R the actor wakes up and unstashes all the message it has been holding since it felt asleep.
 - If an actor is awake it forwards (or drops if it was the sender) the message basing on its direction attribute and increments its passed messages counter (when it reaches W the actor goes to rest) unless the already held message attribute of the message is set to true.
