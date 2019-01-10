package akka_examples;

import java.util.Random;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.japi.pf.DeciderBuilder;
import akka.japi.pf.ReceiveBuilder;
import scala.concurrent.duration.Duration;

public final class SupervisorActorExample {

    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create();
        final ActorRef supervisor = system.actorOf(Supervisor.props(), "supervisor");
        for (int i=0; i < 10; i++) {
            supervisor.tell(new UnstableActor.Command(), ActorRef.noSender());
        }
        system.awaitTermination();
    }
}

class Supervisor extends AbstractLoggingActor {
    public static final OneForOneStrategy STRATEGY = new OneForOneStrategy( // each child is treated separately
            10, // max 10 restart per 10 seconds
            Duration.create("10 seconds"),
            DeciderBuilder
                    .match(RuntimeException.class, ex -> SupervisorStrategy.restart())
                    .build()
    );


    public Supervisor() {
        final ActorRef unstableActor = getContext().actorOf(UnstableActor.props(), "unstableActor");
        receive(ReceiveBuilder
                .matchAny(any -> unstableActor.forward(any, getContext()))
                .build()
        );
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return STRATEGY;
    }

    public static Props props() {
        return Props.create(Supervisor.class);
    }
}

class UnstableActor extends AbstractLoggingActor {

    public static class Command {}

    public UnstableActor() {
        log().info("Creating UnstableActor...");
        receive(ReceiveBuilder
                .match(Command.class, this::onCommand)
                .build()
        );
    }

    private void onCommand(Command c) {
        int rng = new Random().nextInt(10);
        if (rng < 9) {
            throw new RuntimeException("Woops, something went wrong");
        }
        log().info("Handling the command...");
    }

    public static Props props() {
        return Props.create(UnstableActor.class);
    }

    @Override
    public void postStop() throws Exception {
        log().info("Stopping UnstableActor...");
    }
}
