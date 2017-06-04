package fed;

import amb.Ambasador;
import fom.FomInteraction;
import hla.rti.*;
import hla.rti.jlc.RtiFactoryFactory;

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
        achieveSyncPoint();
        //timePolicy(); //może to miało być zrobione?
        enableTimePolicy();
        publishAndSubscribe();
        registerObjects();

        while (fedamb.running) {
            double timeToAdvance = fedamb.federateTime + timeStep;
            advanceTime(timeToAdvance);

            /*if (fedamb.externalEvents.size() > 0) {
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
            }*/
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
            FomInteraction interaction = new FomInteraction(addClientServicedHandle);
            interaction.addAttributeHandle(NR_KASY, addClientServicedHandle, Integer.class);
            interaction.addAttributeHandle(NR_KLIENTA, addClientServicedHandle, Integer.class);
            rtiamb.publishInteractionClass(addClientServicedHandle);

            int addCashEntryHandle = rtiamb.getInteractionClassHandle("InteractionRoot.wejscieDoKasy");
            interaction = new FomInteraction(addCashEntryHandle);
            interaction.addAttributeHandle(NR_KASY, addCashEntryHandle, Integer.class);
            interaction.addAttributeHandle(NR_KLIENTA, addCashEntryHandle, Integer.class);
            rtiamb.publishInteractionClass(addCashEntryHandle);

            int addQueueExitHandle = rtiamb.getInteractionClassHandle("InteractionRoot.opuszczenieKolejki");
            interaction = new FomInteraction(addQueueExitHandle);
            interaction.addAttributeHandle(NR_KASY, addQueueExitHandle, Integer.class);
            interaction.addAttributeHandle(NR_KLIENTA, addQueueExitHandle, Integer.class);
            rtiamb.subscribeInteractionClass(addQueueExitHandle);

            int addQueueEntryHandle = rtiamb.getInteractionClassHandle("InteractionRoot.wejscieDoKolejki");
            interaction = new FomInteraction(addQueueEntryHandle);
            interaction.addAttributeHandle(NR_KASY, addQueueEntryHandle, Integer.class);
            interaction.addAttributeHandle(NR_KLIENTA, addQueueEntryHandle, Integer.class);
            rtiamb.subscribeInteractionClass(addQueueEntryHandle);

            int addNewCashHandle = rtiamb.getInteractionClassHandle("InteractionRoot.otworzKase");
            interaction = new FomInteraction(addNewCashHandle);
            interaction.addAttributeHandle(NR_KASY, addNewCashHandle, Integer.class);
            rtiamb.subscribeInteractionClass(addNewCashHandle);

            int addCloseCashHandle = rtiamb.getInteractionClassHandle("InteractionRoot.zamknijKase");
            interaction = new FomInteraction(addCloseCashHandle);
            interaction.addAttributeHandle(NR_KASY, addCloseCashHandle, Integer.class);
            rtiamb.subscribeInteractionClass(addCloseCashHandle);

            int addSimulationStartHandle = rtiamb.getInteractionClassHandle("InteractionRoot.startSymulacji");
            rtiamb.subscribeInteractionClass(addSimulationStartHandle);

            int addSimulationStopHandle = rtiamb.getInteractionClassHandle("InteractionRoot.stopSymulacji");
            rtiamb.subscribeInteractionClass(addSimulationStopHandle);
        } catch (NameNotFound | FederateNotExecutionMember | RTIinternalError | AttributeNotDefined | OwnershipAcquisitionPending | InteractionClassNotDefined | SaveInProgress | ConcurrentAccessAttempted | RestoreInProgress | FederateLoggingServiceCalls | ObjectClassNotDefined nameNotFound) {
            nameNotFound.printStackTrace();
        }
    }

    public void deleteObjects() {
    }
    public void registerObjects() {
    }

    /*@Override
    public void registerObjects() {

    }

    @Override
    protected void deleteObjects() {

    }*/
}
