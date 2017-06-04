package fed;
/**
 * Created by konrad on 5/28/17.
 */

import amb.Ambasador;
import fom.FomInteraction;
import fom.Pair;
import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;
import shared.Klient;

import java.util.*;

public class FederatKlient extends AbstractFederat {
    private static final String federateName = "FederatKlient";


    private Random rand = new Random();
    private float generatingChance = .99f;

    private List<Klient> allCustomers = new ArrayList<>();
    private Map<Integer, Integer> queuesSizes = new HashMap<>();
    private Map<Integer, Klient> customersHandlesToObjects = new HashMap<>();
    private Map<Klient, Integer> customersObjectsToHandles = new HashMap<>();
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
        achieveSyncPoint();
        enableTimePolicy();
        publishAndSubscribe();
        registerObjects();

        while (fedamb.running) {
            if (fedamb.isSimulationStarted()) {
                //double timeToAdvance = fedamb.federateTime + timeStep;
                double federateTime = getFederateAmbassador().getFederateTime();
                log("dupa");
                if (rand.nextFloat() < generatingChance)
                    createAndRegisterCustomer(federateTime);
                updateCustomersWithNewFederateTime(federateTime);

                //logika obslugi wszystkich klientow
            }
            advanceTime(timeStep);

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
                    if (classHandle.equals(fedamb.kasaClassHandle.getClassHandle())) {
                        log("Checkout " + objectHandle + " updated, updating queue size");
                        for (int i = 0; i < theAttributes.size(); i++) {
                            int attributeHandle;
                            try {
                                attributeHandle = theAttributes.getAttributeHandle(i);
                                Class<?> typeFor = fedamb.kasaClassHandle.getTypeFor(attributeHandle);
                                if (isAttributeQueueLength(attributeHandle, typeFor)) {
                                    updateCheckoutQueueWith(objectHandle, theAttributes.getValue(i));
                                }
                            } catch (Exception e) {
                                log(e.getMessage());
                            }
                        }
                    }
                });
        fedAmbassador.registerInteractionReceivedListener((int interactionClass, ReceivedInteraction theInteraction, byte[] tag, LogicalTime theTime, EventRetractionHandle eventRetractionHandle) -> {
            if (interactionClass == fedamb.startSymulacjiClassHandle.getClassHandle()) {
                log("Start interaction received");
                fedamb.setSimulationStarted(true);
            }else
            if (interactionClass == fedamb.stopSymulacjiClassHandle.getClassHandle()) {
                log("Stop interaction received");
                fedamb.running = false;
            }
        });
        return fedAmbassador;
    }

    private void updateCheckoutQueueWith(int objectHandle, byte[] value) throws ArrayIndexOutOfBounds {
        queuesSizes.put(objectHandle, EncodingHelpers.decodeInt(value));
    }

    private void updateCustomersWithNewFederateTime(double newFederateTime) {
        allCustomers.forEach(customer -> {
            customer.updateWithNewFederateTime(newFederateTime);
        });
    }

    private void sendQueueEnteredInteraction(Integer customerObjectId, Integer checkoutObjectId, boolean privileged) {
        SuppliedParameters parameters;
        try {
            parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
            parameters.add(fedamb.wejscieDoKolejkiClassHandle.getHandleFor(NR_KLIENTA),
                    EncodingHelpers.encodeInt(customerObjectId));
            parameters.add(fedamb.wejscieDoKolejkiClassHandle.getHandleFor(NR_KASY),
                    EncodingHelpers.encodeInt(checkoutObjectId));
            parameters.add(fedamb.wejscieDoKolejkiClassHandle.getHandleFor(UPRZYWILEJOWANY),
                    EncodingHelpers.encodeBoolean(privileged));
            rtiamb.sendInteraction(fedamb.wejscieDoKolejkiClassHandle.getClassHandle(), parameters, generateTag());
        } catch (RTIexception e) {
            log("Couldn't send queue entered interaction, because: " + e.getMessage());
        }
    }

    private boolean isAttributeQueueLength(int attributeHandle, Class<?> typeFor) {
        return typeFor.getName().equalsIgnoreCase(Integer.class.getName())
                && fedamb.kasaClassHandle.getNameFor(attributeHandle).equalsIgnoreCase(DLUGOSC_KOLEJKI);
    }

    private void createAndRegisterCustomer(double oldFederateTime) {
        Klient customer = new Klient(oldFederateTime, rand.nextInt(MAX_SHOPPING_TIME - MIN_SHOPPING_TIME + 1) + MIN_SHOPPING_TIME);
        customer.setPrivileged(rand.nextBoolean());
        allCustomers.add(customer);
        try {
            int customerHandle = registerRtiCustomer(customer);
            log("New customer " + customerHandle + " enters the bank: " + customer);
        } catch (RTIexception e) {
            log("Couldn't create new customer, because: " + e.getMessage());
        }
    }

    private int registerRtiCustomer(Klient customer) throws RTIexception {
        int customerHandle = rtiamb.registerObjectInstance(fedamb.klientClassHandle.getClassHandle());
        SuppliedAttributes attributes = RtiFactoryFactory.getRtiFactory().createSuppliedAttributes();
        attributes.add(fedamb.klientClassHandle.getHandleFor(UPRZYWILEJOWANY),
                EncodingHelpers.encodeBoolean(customer.isPrivileged()));
        rtiamb.updateAttributeValues(customerHandle, attributes, generateTag());
        customersHandlesToObjects.put(customerHandle, customer);
        customersObjectsToHandles.put(customer, customerHandle);
        return customerHandle;
    }

    public void publishAndSubscribe() {
        try {
            /*int addQueueEntryHandle = rtiamb.getInteractionClassHandle(HLA_WEJSCIE_KLIENT);
            fedamb.wejscieDoKolejkiClassHandle = new FomInteraction(addQueueEntryHandle);
            fedamb.wejscieDoKolejkiClassHandle.addAttributeHandle(NR_KASY, addQueueEntryHandle, Integer.class);
            fedamb.wejscieDoKolejkiClassHandle.addAttributeHandle(NR_KLIENTA, addQueueEntryHandle, Integer.class);
            rtiamb.publishInteractionClass(fedamb.wejscieDoKolejkiClassHandle.getClassHandle());*/

            /*int addClientHandle = rtiamb.getObjectClassHandle(HLA_KLIENT);
            klientHandle = new FomObject(addClientHandle);
            klientHandle.addAttributeHandle(NR_KASY, addClientHandle, Integer.class);
            klientHandle.addAttributeHandle(NR_KLIENTA, addClientHandle, Integer.class);
            klientHandle.addAttributeHandle(POZYCJA_KOLEJKI, addClientHandle, Integer.class);
            klientHandle.addAttributeHandle(CZY_UPRZYWILEJOWANY, addClientHandle, Boolean.class);
            klientHandle.addAttributeHandle(RODZAJ_ZALATWIANEJ_SPRAWY, addClientHandle, Integer.class);
            rtiamb.publishObjectClass(klientHandle.getClassHandle(), klientHandle.createAttributeHandleSet());*/

            fedamb.klientClassHandle = prepareFomObject(rtiamb.getObjectClassHandle(HLA_KLIENT),
                    new Pair<String, Class<?>>(NR_KASY, Integer.class),
                    new Pair<String, Class<?>>(NR_KLIENTA, Integer.class),
                    new Pair<String, Class<?>>(CZY_UPRZYWILEJOWANY, Boolean.class),
                    new Pair<String, Class<?>>(POZYCJA_KOLEJKI, Integer.class),
                    new Pair<String, Class<?>>(RODZAJ_ZALATWIANEJ_SPRAWY, Integer.class));
            rtiamb.publishObjectClass(fedamb.klientClassHandle.getClassHandle(),
                    fedamb.klientClassHandle.createAttributeHandleSet());

            /*int addQueueExitHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.opuszczenieKolejki");
            interaction = new FomInteraction(addQueueExitHandle);
            interaction.addAttributeHandle(NR_KASY, addQueueEntryHandle, Integer.class);
            interaction.addAttributeHandle(NR_KLIENTA, addQueueEntryHandle, Integer.class);
            rtiamb.publishInteractionClass(addQueueExitHandle);

            int addServicedHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.obsluzonoKlienta");
            interaction = new FomInteraction(addServicedHandle);
            interaction.addAttributeHandle(NR_KASY, addQueueEntryHandle, Integer.class);
            interaction.addAttributeHandle(NR_KLIENTA, addQueueEntryHandle, Integer.class);
            rtiamb.subscribeInteractionClass(addServicedHandle);

            int addNewClientHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.nowyKlient");
            interaction = new FomInteraction(addNewClientHandle);
            interaction.addAttributeHandle(NR_KLIENTA, addQueueEntryHandle, Integer.class);
            rtiamb.subscribeInteractionClass(addNewClientHandle);

            int addCashEntryHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.wejscieDoKasy");
            interaction = new FomInteraction(addCashEntryHandle);
            interaction.addAttributeHandle(NR_KASY, addQueueEntryHandle, Integer.class);
            interaction.addAttributeHandle(NR_KLIENTA, addQueueEntryHandle, Integer.class);
            rtiamb.subscribeInteractionClass(addCashEntryHandle);
            */
            fedamb.stopSymulacjiClassHandle = prepareFomInteraction(rtiamb.getInteractionClassHandle(HLA_STOP_SIM));
            rtiamb.subscribeInteractionClass(fedamb.stopSymulacjiClassHandle.getClassHandle());

            fedamb.startSymulacjiClassHandle = prepareFomInteraction(rtiamb.getInteractionClassHandle(HLA_START_SIM));
            rtiamb.subscribeInteractionClass(fedamb.startSymulacjiClassHandle.getClassHandle());
        } catch
                (NameNotFound | FederateNotExecutionMember | RTIinternalError | InteractionClassNotDefined | SaveInProgress | ConcurrentAccessAttempted | RestoreInProgress | FederateLoggingServiceCalls | OwnershipAcquisitionPending | ObjectClassNotDefined | AttributeNotDefined
                        nameNotFound) {
            nameNotFound.printStackTrace();
        }
    }

    public void deleteObjects() {
    }

    public void registerObjects() {
    }

}
