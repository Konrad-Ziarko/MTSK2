/**
 * Created by konrad on 5/28/17.
 */

import hla.rti.*;
import hla.rti.jlc.RtiFactoryFactory;
import org.portico.impl.hla13.types.DoubleTime;
import org.portico.impl.hla13.types.DoubleTimeInterval;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Collections;

public class KlientF {
    public static final String READY_TO_RUN = "ReadyToRun";
    private RTIambassador rtiamb;
    private final double timeStep = 10.0;
    private KlientA fedamb;
    private int klientHandle;

    public static void main(String[] args) {
        try {
            new KlientF().runFederate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void log(String message) {
        System.out.println("KlientFederate   : " + message);
    }

    private LogicalTimeInterval convertInterval(double time) {
        return new DoubleTimeInterval(time);
    }

    private LogicalTime convertTime(double time) {
        return new DoubleTime(time);
    }

    private void runFederate() {

        try {
            rtiamb = RtiFactoryFactory.getRtiFactory().createRtiAmbassador();
        } catch (RTIinternalError rtIinternalError) {
            rtIinternalError.printStackTrace();
        }
        try {
            File fom = new File("bank.fed");
            rtiamb.createFederationExecution("ExampleFederation",
                    fom.toURI().toURL());
            log("Created Federation");
        } catch (FederationExecutionAlreadyExists exists) {
            log("Didn't create federation, it already existed");
        } catch (MalformedURLException urle) {
            log("Exception processing fom: " + urle.getMessage());
            urle.printStackTrace();
            return;
        } catch (ConcurrentAccessAttempted | RTIinternalError | CouldNotOpenFED | ErrorReadingFED concurrentAccessAttempted) {
            concurrentAccessAttempted.printStackTrace();
        }

        fedamb = new KlientA();
        try {
            rtiamb.joinFederationExecution("KlientFederate", "ExampleFederation", fedamb);
        } catch (FederateAlreadyExecutionMember | FederationExecutionDoesNotExist | SaveInProgress | RTIinternalError | RestoreInProgress | ConcurrentAccessAttempted federateAlreadyExecutionMember) {
            federateAlreadyExecutionMember.printStackTrace();
        }
        log("Joined Federation as KlientFederate");

        try {
            rtiamb.registerFederationSynchronizationPoint(READY_TO_RUN, null);
        } catch (FederateNotExecutionMember | SaveInProgress | RTIinternalError | RestoreInProgress | ConcurrentAccessAttempted federateNotExecutionMember) {
            federateNotExecutionMember.printStackTrace();
        }

        while (fedamb.isAnnounced == false) {
            try {
                rtiamb.tick();
            } catch (RTIinternalError | ConcurrentAccessAttempted rtIinternalError) {
                rtIinternalError.printStackTrace();
            }
        }
        try {
            rtiamb.synchronizationPointAchieved(READY_TO_RUN);
        } catch (SynchronizationLabelNotAnnounced | ConcurrentAccessAttempted | RTIinternalError | RestoreInProgress | SaveInProgress | FederateNotExecutionMember synchronizationLabelNotAnnounced) {
            synchronizationLabelNotAnnounced.printStackTrace();
        }
        log("Achieved sync point: " + READY_TO_RUN + ", waiting for federation...");
        while (fedamb.isReadyToRun == false) {
            try {
                rtiamb.tick();
            } catch (RTIinternalError | ConcurrentAccessAttempted rtIinternalError) {
                rtIinternalError.printStackTrace();
            }
        }

        try {
            enableTimePolicy();
        } catch (RTIexception rtIexception) {
            rtIexception.printStackTrace();
        }

        try {
            publishAndSubscribe();
        } catch (RTIexception rtIexception) {
            rtIexception.printStackTrace();
        }

        try {
            registerKlientObject();
        } catch (RTIexception rtIexception) {
            rtIexception.printStackTrace();
        }

        while (fedamb.running) {
            double timeToAdvance = fedamb.federateTime + timeStep;
            advanceTime(timeToAdvance);

            if (fedamb.externalEvents.size() > 0) {
                Collections.sort(fedamb.externalEvents, new ExternalEvent.ExternalEventComparator());
                for (ExternalEvent externalEvent : fedamb.externalEvents) {
                    fedamb.federateTime = externalEvent.getTime();
                    switch (externalEvent.getEventType()) {
                        case ADD:
                            this.addToStock(externalEvent.getQty());
                            break;
                    }
                }
                fedamb.externalEvents.clear();
            }

            if (fedamb.grantedTime == timeToAdvance) {
                timeToAdvance += fedamb.federateLookahead;
                log("Updating stock at time: " + timeToAdvance);
                updateHLAObject(timeToAdvance);
                fedamb.federateTime = timeToAdvance;
            }

            rtiamb.tick();
        }
    }

    private void enableTimePolicy() throws RTIexception {
        LogicalTime currentTime = convertTime(fedamb.federateTime);
        LogicalTimeInterval lookahead = convertInterval(fedamb.federateLookahead);

        this.rtiamb.enableTimeRegulation(currentTime, lookahead);

        while (fedamb.isRegulating == false) {
            rtiamb.tick();
        }

        this.rtiamb.enableTimeConstrained();

        while (fedamb.isConstrained == false) {
            rtiamb.tick();
        }
    }

    private void publishAndSubscribe() throws RTIexception {

        int classHandle = rtiamb.getObjectClassHandle("ObjectRoot.Storage");
        int stockHandle = rtiamb.getAttributeHandle("stock", classHandle);

        AttributeHandleSet attributes =
                RtiFactoryFactory.getRtiFactory().createAttributeHandleSet();
        attributes.add(stockHandle);

        rtiamb.publishObjectClass(classHandle, attributes);

        int addProductHandle = rtiamb.getInteractionClassHandle("InteractionRoot.AddProduct");
        fedamb.addProductHandle = addProductHandle;
        rtiamb.subscribeInteractionClass(addProductHandle);
    }

    private void registerKlientObject() throws RTIexception {
        int classHandle = rtiamb.getObjectClassHandle("ObjectRoot.Klient");
        this.klientHandle = rtiamb.registerObjectInstance(classHandle);
    }

    private void advanceTime(double timeToAdvance) throws RTIexception {
        fedamb.isAdvancing = true;
        LogicalTime newTime = convertTime(timeToAdvance);
        rtiamb.timeAdvanceRequest(newTime);

        while (fedamb.isAdvancing) {
            rtiamb.tick();
        }
    }
}
