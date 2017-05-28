package fed; /**
 * Created by konrad on 5/28/17.
 */

import amb.KlientA;
import hla.rti.*;
import hla.rti.jlc.RtiFactoryFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Collections;

public class KlientF extends Federat {
    private int klientHandle;

    public static void main(String[] args) {
        try {
            new KlientF().runFederate();
        } catch (Exception e) {
            e.printStackTrace();
        }
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

            try {
                rtiamb.tick();
            } catch (RTIinternalError | ConcurrentAccessAttempted rtIinternalError) {
                rtIinternalError.printStackTrace();
            }
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
}
