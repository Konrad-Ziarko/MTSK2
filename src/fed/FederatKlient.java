package fed;
/**
 * Created by konrad on 5/28/17.
 */

import amb.Ambasador;
import fom.FomInteraction;
import hla.rti.*;

public class FederatKlient extends AbstractFederat {
    private int klientHandle;
    private static final String federateName = "KlientFederate";

    public static void main(String[] args) {
        new FederatKlient().runFederate();
    }

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

            //logika obslugi wszystkich klientow

            advanceTime(timeToAdvance);
        }
    }

    public void publishAndSubscribe() {
        try {

            int addQueueEntryHandle = rtiamb.getInteractionClassHandle("InteractionRoot.wejscieDoKolejki");
            FomInteraction interaction = new FomInteraction(addQueueEntryHandle);
            interaction.addAttributeHandle(NR_KASY, addQueueEntryHandle, Integer.class);
            interaction.addAttributeHandle(NR_KLIENTA, addQueueEntryHandle, Integer.class);
            rtiamb.publishInteractionClass(addQueueEntryHandle);

            int addQueueExitHandle = rtiamb.getInteractionClassHandle("InteractionRoot.opuszczenieKolejki");
            interaction = new FomInteraction(addQueueExitHandle);
            interaction.addAttributeHandle(NR_KASY, addQueueEntryHandle, Integer.class);
            interaction.addAttributeHandle(NR_KLIENTA, addQueueEntryHandle, Integer.class);
            rtiamb.publishInteractionClass(addQueueExitHandle);

            int addServicedHandle = rtiamb.getInteractionClassHandle("InteractionRoot.obsluzonoKlienta");
            interaction = new FomInteraction(addServicedHandle);
            interaction.addAttributeHandle(NR_KASY, addQueueEntryHandle, Integer.class);
            interaction.addAttributeHandle(NR_KLIENTA, addQueueEntryHandle, Integer.class);
            rtiamb.subscribeInteractionClass(addServicedHandle);

            int addNewClientHandle = rtiamb.getInteractionClassHandle("InteractionRoot.nowyKlient");
            interaction = new FomInteraction(addNewClientHandle);
            interaction.addAttributeHandle(NR_KLIENTA, addQueueEntryHandle, Integer.class);
            rtiamb.subscribeInteractionClass(addNewClientHandle);

            int addCashEntryHandle = rtiamb.getInteractionClassHandle("InteractionRoot.wejscieDoKasy");
            interaction = new FomInteraction(addCashEntryHandle);
            interaction.addAttributeHandle(NR_KASY, addQueueEntryHandle, Integer.class);
            interaction.addAttributeHandle(NR_KLIENTA, addQueueEntryHandle, Integer.class);
            rtiamb.subscribeInteractionClass(addCashEntryHandle);

            int addSimulationStartHandle = rtiamb.getInteractionClassHandle("InteractionRoot.startSymulacji");
            rtiamb.subscribeInteractionClass(addSimulationStartHandle);

            int addSimulationStopHandle = rtiamb.getInteractionClassHandle("InteractionRoot.stopSymulacji");
            rtiamb.subscribeInteractionClass(addSimulationStopHandle);
        } catch
                (NameNotFound | FederateNotExecutionMember | RTIinternalError | InteractionClassNotDefined | SaveInProgress | ConcurrentAccessAttempted | RestoreInProgress | FederateLoggingServiceCalls
                        nameNotFound) {
            nameNotFound.printStackTrace();
        }
    }

    public void deleteObjects() {
    }

    /*protected void deleteObjects() throws RTIexception {
        log("Deleting " + customersHandlesToObjects.size() + " created customer objects");
        customersHandlesToObjects.keySet().stream().forEach(handle -> {
            try {
                rtiAmbassador.deleteObjectInstance(handle, generateTag());
            } catch (Exception e) {
                log("Couldn't delete " + handle + ", because: " + e.getMessage());
            }
        });
    }*/
    public void registerObjects() {
    }
    /*public void registerObjects() {
        int classHandle = 0;
        try {
            classHandle = rtiamb.getObjectClassHandle("ObjectRoot.Klient");
            this.klientHandle = rtiamb.registerObjectInstance(classHandle);
        } catch (NameNotFound | FederateNotExecutionMember | SaveInProgress | RTIinternalError | ObjectClassNotDefined | ConcurrentAccessAttempted | ObjectClassNotPublished | RestoreInProgress nameNotFound) {
            nameNotFound.printStackTrace();
        }
    }*/

    public void waitForSyncPoint() {

    }
}
