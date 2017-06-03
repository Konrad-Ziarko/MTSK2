package fed;
/**
 * Created by konrad on 5/28/17.
 */

import amb.Ambasador;
import fom.FomInteraction;
import fom.FomObject;
import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;
import shared.Klient;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class FederatKlient extends AbstractFederat {
    private static final String federateName = "KlientFederate";
    private FomObject klientHandle;
    private FomObject kasaHandle;
    private FomInteraction wejscieDoKolejkiHandle;
    private FomInteraction stopSymulacjiHandle;

    private Random rand = new Random();
    private float generatingChance = .5f;

    private List<Klient> allCustomers;
    private Map<Integer, Integer> queuesSizes;
    private Map<Integer, Klient> customersHandlesToObjects;
    private Map<Klient, Integer> customersObjectsToHandles;
    private int MAX_SHOPPING_TIME = 10;
    private int MIN_SHOPPING_TIME = 5;

    public static void main(String[] args) {
        new FederatKlient().runFederate();
    }

    public void runFederate() {
        createFederation();
        fedamb = prepareFederationAmbassador();
        joinFederation(federateName);
        registerSyncPoint();
        waitForSyncPoint();
        achieveSyncPoint();
        enableTimePolicy();
        publishAndSubscribe();
        registerObjects();

        while (fedamb.running) {
            double timeToAdvance = fedamb.federateTime + timeStep;
            double federateTime = getFederateAmbassador().getFederateTime();

            if (rand.nextFloat() < generatingChance)
                createAndRegisterCustomer(federateTime);
            //logika obslugi wszystkich klientow

            advanceTime(timeToAdvance);
        }
    }

    private Ambasador prepareFederationAmbassador() {
        Ambasador fedAmbassador = new Ambasador();
        fedAmbassador.registerObjectInstanceCreatedListener((objectHandle, classHandle, objectName) -> {
            objectToClassHandleMap.put(objectHandle, classHandle);
            queuesSizes.put(objectHandle, 0);
        });
        fedAmbassador
                .registerAttributesUpdatedListener((objectHandle, theAttributes, tag, theTime, retractionHandle) -> {
                    Integer classHandle = objectToClassHandleMap.get(objectHandle);
                    if (classHandle.equals(kasaHandle.getClassHandle())) {
                        log("Checkout " + objectHandle + " updated, updating queue size");
                        for (int i = 0; i < theAttributes.size(); i++) {
                            int attributeHandle;
                            try {
                                attributeHandle = theAttributes.getAttributeHandle(i);
                                Class<?> typeFor = kasaHandle.getTypeFor(attributeHandle);
                                if (isAttributeQueueLength(attributeHandle, typeFor)) {
                                    updateCheckoutQueueWith(objectHandle, theAttributes.getValue(i));
                                }
                            } catch (Exception e) {
                                log(e.getMessage());
                            }
                        }
                    }
                });
        fedAmbassador.registerInteractionReceivedListener((int interactionClass, ReceivedInteraction theInteraction,
                                                           byte[] tag, LogicalTime theTime, EventRetractionHandle eventRetractionHandle) -> {
            if (interactionClass == stopSymulacjiHandle.getClassHandle()) {
                log("Stop interaction received");
                fedamb.running = false;
            }
        });
        return fedAmbassador;
    }

    private void updateCheckoutQueueWith(int objectHandle, byte[] value) throws ArrayIndexOutOfBounds {
        queuesSizes.put(objectHandle, EncodingHelpers.decodeInt(value));
    }

    private boolean isAttributeQueueLength(int attributeHandle, Class<?> typeFor) {
        return typeFor.getName().equalsIgnoreCase(Integer.class.getName())
                && kasaHandle.getNameFor(attributeHandle).equalsIgnoreCase(DLUGOSC_KOLEJKI);
    }

    private void createAndRegisterCustomer(double oldFederateTime) {
        Klient customer = new Klient(oldFederateTime, rand.nextInt(MAX_SHOPPING_TIME - MIN_SHOPPING_TIME + 1) + MIN_SHOPPING_TIME);
        customer.setPrivileged(rand.nextBoolean());
        allCustomers.add(customer);
        try {
            int customerHandle = registerRtiCustomer(customer);
            log("New customer " + customerHandle + " enters the market: " + customer);
        } catch (RTIexception e) {
            log("Couldn't create new customer, because: " + e.getMessage());
        }
    }

    private int registerRtiCustomer(Klient customer) throws RTIexception {
        int customerHandle = rtiamb.registerObjectInstance(klientHandle.getClassHandle());
        SuppliedAttributes attributes = RtiFactoryFactory.getRtiFactory().createSuppliedAttributes();
        attributes.add(klientHandle.getHandleFor(UPRZYWILEJOWANY),
                EncodingHelpers.encodeBoolean(customer.isPrivileged()));
        rtiamb.updateAttributeValues(customerHandle, attributes, generateTag());
        customersHandlesToObjects.put(customerHandle, customer);
        customersObjectsToHandles.put(customer, customerHandle);
        return customerHandle;
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
