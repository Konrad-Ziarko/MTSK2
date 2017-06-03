package fed;
/**
 * Created by konrad on 5/28/17.
 */

import amb.Ambasador;
import fom.FomInteraction;
import fom.FomObject;
import hla.rti.*;
import shared.Klient;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class FederatKlient extends AbstractFederat {
    private static final String federateName = "KlientFederate";
    private FomObject klientHandle;
    private FomObject kasaHandle;
    private FomInteraction wejscieDoKolejkiHandle;

    private Random customerGenerator = new Random();
    private float generatingChance = .5f;

    private List<Klient> allCustomers;
    private Map<Integer, Integer> queuesSizes;
    private Map<Integer, Klient> customersHandlesToObjects;
    private Map<Klient, Integer> customersObjectsToHandles;

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
            wejscieDoKolejkiHandle = new FomInteraction(addQueueEntryHandle);
            wejscieDoKolejkiHandle.addAttributeHandle(NR_KASY, addQueueEntryHandle, Integer.class);
            wejscieDoKolejkiHandle.addAttributeHandle(NR_KLIENTA, addQueueEntryHandle, Integer.class);
            rtiamb.publishInteractionClass(wejscieDoKolejkiHandle.getClassHandle());

            /*int addQueueExitHandle = rtiamb.getInteractionClassHandle("InteractionRoot.opuszczenieKolejki");
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
            */
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
