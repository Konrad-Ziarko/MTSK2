package fed;

import amb.Ambasador;
import hla.rti.*;
import hla.rti.jlc.RtiFactoryFactory;

import java.util.Collections;

/**
 * Created by konrad on 5/28/17.
 */
public class FederatKasa extends AbstractFederat {
    private static final String federateName = "KasaFederate";


    @Override
    public void runFederate() {
        createFederation();
        fedamb = new Ambasador();
        joinFederation(federateName);
        registerSyncPoint();
        waitForSyncPoint();
        achieveSyncPoint();
        //timePolicy(); //może to miało być zrobione?
        enableTimePolicy();
        publishAndSubscribe();
        registerObjects();

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

    @Override
    public void publishAndSubscribe() {
        int classHandle = 0;
        try {
            classHandle = rtiamb.getObjectClassHandle("ObjectRoot.Kasa");
            int cashNumberHandle = rtiamb.getAttributeHandle("nrKasy", classHandle);
            int clientNumberHandle = rtiamb.getAttributeHandle("nrObslugiwanegoKlienta", classHandle);
            AttributeHandleSet attributes =
                    RtiFactoryFactory.getRtiFactory().createAttributeHandleSet();
            attributes.add(cashNumberHandle);
            attributes.add(clientNumberHandle);

            rtiamb.publishObjectClass(classHandle, attributes);

            int addClientServicedHandle = rtiamb.getInteractionClassHandle("InteractionRoot.obsluzonoKlienta");
            rtiamb.publishInteractionClass(addClientServicedHandle);

            int addCashEntryHandle = rtiamb.getInteractionClassHandle("InteractionRoot.wejscieDoKasy");
            rtiamb.publishInteractionClass(addCashEntryHandle);

            int addQueueExitHandle = rtiamb.getInteractionClassHandle("InteractionRoot.opuszczenieKolejki");
            rtiamb.subscribeInteractionClass(addQueueExitHandle);

            int addQueueEntryHandle = rtiamb.getInteractionClassHandle("InteractionRoot.wejscieDoKolejki");
            rtiamb.subscribeInteractionClass(addQueueEntryHandle);

            int addNewCashHandle = rtiamb.getInteractionClassHandle("InteractionRoot.otworzKase");
            rtiamb.subscribeInteractionClass(addNewCashHandle);

            int addCloseCashHandle = rtiamb.getInteractionClassHandle("InteractionRoot.zamknijKase");
            rtiamb.subscribeInteractionClass(addCloseCashHandle);

            int addSimulationStartHandle = rtiamb.getInteractionClassHandle("InteractionRoot.startSymulacji");
            rtiamb.subscribeInteractionClass(addSimulationStartHandle);

            int addSimulationStopHandle = rtiamb.getInteractionClassHandle("InteractionRoot.stopSymulacji");
            rtiamb.subscribeInteractionClass(addSimulationStopHandle);
        } catch (NameNotFound | FederateNotExecutionMember | RTIinternalError | AttributeNotDefined | OwnershipAcquisitionPending | InteractionClassNotDefined | SaveInProgress | ConcurrentAccessAttempted | RestoreInProgress | FederateLoggingServiceCalls | ObjectClassNotDefined nameNotFound) {
            nameNotFound.printStackTrace();
        }
    }

    @Override
    public void waitForSyncPoint() {

    }

    /*@Override
    public void registerObjects() {

    }

    @Override
    protected void deleteObjects() {

    }*/
}
