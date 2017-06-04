package fed;

import amb.Ambasador;
import fom.AFomEntity;
import fom.FomInteraction;
import fom.FomObject;
import fom.Pair;
import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * Created by konrad on 5/28/17.
 */
public class FederatGUI extends AbstractFederat {
    private Logger logger = LoggerFactory.getLogger(getClass());
    private static final String CZAS_OBSLUGI = "sredniCzasObslugi";
    private static final String CZAS_CZEKANIA_NA_OBSLUGE = "sredniCzasOczekiwania";
    private static final String NR_OBSLUGIWANEGO = "nrObslugiwanegoKlienta";
    private static final String ID_KASA = "idKasa";
    private static final String ID_KLIENT = "idKlient";
    private static final String NR_KASY = "nrKasy";
    private static final String NR_W_KOLEJCE = "pozycjaKolejki";
    private static final String UPRZYWILEJOWANY = "czyUprzywilejowany";
    private static final String CZAS_ZAKUPOW = "rodzajZalatwianejSprawy";
    private FomObject kasaClassHandle;
    private FomObject klientClassHandle;
    private FomInteraction rozpoczecieObslugiClassHandle;
    private FomInteraction koniecObslugiClassHandle;
    private FomInteraction wejscieDoKolejkiClassHandle;
    private FomInteraction otworzKaseClassHandle;
    private boolean shouldProceed = false;
    private Ambasador fedAmbassador;
    private GUIFederateController controller;
    private Map<Integer, Integer> checkoutObjectHandleToClassHandleMap;
    private Map<Integer, Integer> customerObjectHandleToClassHandleMap;
    private Map<Integer, Pair<Integer, Boolean>> checkoutObjectHandleToQueueSizeAndFilledMap;
    private List<Integer> customers;
    private FomObject statisticsClassHandle;
    private Integer statisticsObjectHandle;
    private int customersLeft = 0;
    private boolean shouldSendCloseTheMarketInteraction = false;
    private FomInteraction closeTheMarketClassHandle;
    private boolean shouldSendStopInteraction = false;
    private AFomEntity stopClassHandle;

    public GUIFederate(String federateName, String federationName, File fomFile, GUIFederateController controller) {
        super(federateName, federationName, fomFile);
        this.controller = controller;
        checkoutObjectHandleToClassHandleMap = new HashMap<>();
        customerObjectHandleToClassHandleMap = new HashMap<>();
        checkoutObjectHandleToQueueSizeAndFilledMap = new HashMap<>();
        customers = new LinkedList<>();
    }

    @Override
    public void runFederate() {
        while (isRunning) {
            controller.setFederationStatus(
                    "Federation " + federationName + " running [" + getFederateAmbassador().getFederateTime() + "]");
            controller.setShoppingCustomers(customers.size());
            controller.setCustomersLeft(customersLeft);
            optionallySendCloseTheMarketInteraction();
            advanceTime(1);
        }
        optionallySendStopFederationInteraction();
    }

    private void optionallySendStopFederationInteraction() {
        if (shouldSendStopInteraction) {
            logger.info("Sending \"stop\" interaction");
            SuppliedParameters parameters;
            try {
                parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
                rtiamb.sendInteraction(stopClassHandle.getClassHandle(), parameters, generateTag());
            } catch (RTIexception e) {
                logger.error("Couldn't send \"stop\" interaction, because: {}", e.getMessage());
            }
            shouldSendStopInteraction = false;
        }
    }

    private void optionallySendCloseTheMarketInteraction() {
        if (shouldSendCloseTheMarketInteraction) {
            logger.info("Sending \"close market\" interaction");
            SuppliedParameters parameters;
            try {
                parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
                rtiamb.sendInteraction(closeTheMarketClassHandle.getClassHandle(), parameters, generateTag());
            } catch (RTIexception e) {
                logger.error("Couldn't send \"close the market\" interaction, because: {}", e.getMessage());
            }
            shouldSendCloseTheMarketInteraction = false;
        }
    }

    @Override
    public void cleanUpFederate() throws RTIexception {
        controller.setFederationStatus("Cleaning up the federate");
        super.cleanUpFederate();
    }

   /* @Override
    protected AbstractFederat getFederateAmbassador() {
        return fedAmbassador;
    }*/

    protected Ambasador prepareFederationAmbassador() {
        fedAmbassador = new Ambasador();
        fedAmbassador.registerObjectInstanceCreatedListener((int theObject, int theObjectClass, String objectName) -> {
            if (theObjectClass == klientClassHandle.getClassHandle()) {
                customerObjectHandleToClassHandleMap.put(theObject, theObjectClass);
                customers.add(theObject);
                logger.info("Customer ({}) entered, customers amount: {}", theObject, customers.size());
            } else if (theObjectClass == kasaClassHandle.getClassHandle()) {
                logger.info("New cash opened ({})", theObject);
                checkoutObjectHandleToClassHandleMap.put(theObject, theObjectClass);
            } else if (theObjectClass == statisticsClassHandle.getClassHandle()) {
                statisticsObjectHandle = theObject;
            }
        });
        fedAmbassador.registerObjectInstanceRemovedListener((int theObject, byte[] userSuppliedTag, LogicalTime theTime,
                                                             EventRetractionHandle retractionHandle) -> {
            if (customerObjectHandleToClassHandleMap.get(theObject) == klientClassHandle.getClassHandle()) {
                customers.remove(new Integer(theObject));
                logger.info("Customer ({}) removed", theObject);
            }
        });
        fedAmbassador.registerInteractionReceivedListener((int interactionClass, ReceivedInteraction theInteraction,
                                                           byte[] tag, LogicalTime theTime, EventRetractionHandle eventRetractionHandle) -> {
            if (interactionClass == wejscieDoKolejkiClassHandle.getClassHandle()) {
                int extractCustomerClassHandle = extractCustomerClassHandle(theInteraction);
                logger.info("Customer({}) entered queue", extractCustomerClassHandle);
                customers.remove(new Integer(extractCustomerClassHandle));
            } else if (interactionClass == koniecObslugiClassHandle.getClassHandle()) {
                controller.setCustomersLeft(++customersLeft);
            }
        });
        fedAmbassador.registerAttributesUpdatedListener((theObject, theAttributes, tag, theTime, whateverMan) -> {
            if (checkoutObjectHandleToClassHandleMap.containsKey(theObject)
                    && kasaClassHandle.getClassHandle() == checkoutObjectHandleToClassHandleMap.get(theObject)) {
                handleCheckoutUpdate(theObject, theAttributes);
            } else if (statisticsObjectHandle != null && statisticsObjectHandle == theObject) {
                extractAndUpdateStatistics(theAttributes);
            }
        });
    }

    private void extractAndUpdateStatistics(ReflectedAttributes theAttributes) {
        double avgShoppingTime = -1;
        double avgWaitingTime = -1;
        double avgServiceTime = -1;
        for (int i = 0; i < theAttributes.size(); i++) {
            try {
                byte[] value = theAttributes.getValue(i);
                if (theAttributes.getAttributeHandle(i) == statisticsClassHandle.getHandleFor("avgShoppingTime")) {
                    avgShoppingTime = EncodingHelpers.decodeDouble(value);
                } else
                if (theAttributes.getAttributeHandle(i) == statisticsClassHandle.getHandleFor("avgWaitingTime")) {
                    avgWaitingTime = EncodingHelpers.decodeDouble(value);
                } else if (theAttributes.getAttributeHandle(i) == statisticsClassHandle
                        .getHandleFor("avgServiceTime")) {
                    avgServiceTime = EncodingHelpers.decodeDouble(value);
                }
            } catch (ArrayIndexOutOfBounds e) {
                logger.error(e.getMessage(), e);
            }
        }
        updateStatistics(avgShoppingTime, avgWaitingTime, avgServiceTime);
    }

    private void updateStatistics(double avgShoppingTime, double avgWaitingTime, double avgServiceTime) {
        controller.setAvgShoppingTime(avgShoppingTime);
        controller.setAvgWaitingTime(avgWaitingTime);
        controller.setAvgServiceTime(avgServiceTime);
    }

    private void handleCheckoutUpdate(int theObject, ReflectedAttributes theAttributes) {
        int queueSize = -1;
        boolean filled = false;
        for (int i = 0; i < theAttributes.size(); i++) {
            try {
                byte[] value = theAttributes.getValue(i);
                if (theAttributes.getAttributeHandle(i) == kasaClassHandle.getHandleFor(DLUGOSC_KOLEJKI)) {
                    queueSize = EncodingHelpers.decodeInt(value);
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        logger.info("Cash ({}) updated: queue size: {}, filled: {}", theObject, queueSize);
        Pair<Integer, Boolean> value = new Pair<>(queueSize, filled);
        checkoutObjectHandleToQueueSizeAndFilledMap.put(theObject, value);
        controller.updateCheckouts(theObject, value);
    }

    private int extractCustomerClassHandle(ReceivedInteraction theInteraction) {
        int handle = -1;
        for (int i = 0; i < theInteraction.size(); i++) {
            try {
                if (theInteraction.getParameterHandle(i) == wejscieDoKolejkiClassHandle.getHandleFor(ID_KLIENT)) {
                    handle = EncodingHelpers.decodeInt(theInteraction.getValue(i));
                }
            } catch (ArrayIndexOutOfBounds e) {
                logger.error(e.getMessage(), e);
            }
        }
        return handle;
    }

    @Override
    protected void waitForUser() {
        logger.info("Waiting until proceed action received from gui");
        controller.setFederationStatus("Waiting for synch point");
        while (!shouldProceed && isRunning) {
            logger.trace("Still waiting...");
        }
        logger.info("Proceed action received from gui, continuing");
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void publishAndSubscribe() {
        try {
            kasaClassHandle = prepareFomObject(rtiamb.getObjectClassHandle("HLAobjectRoot.Kasa"),
                    new Pair<String, Class<?>>(NR_OBSLUGIWANEGO, Integer.class),
                    new Pair<String, Class<?>>(DLUGOSC_KOLEJKI, Integer.class));
            rtiamb.subscribeObjectClassAttributes(kasaClassHandle.getClassHandle(),
                    kasaClassHandle.createAttributeHandleSet());

            klientClassHandle = prepareFomObject(rtiamb.getObjectClassHandle("HLAobjectRoot.Klient"),
                    new Pair<String, Class<?>>(NR_KLIENTA, Integer.class),
                    new Pair<String, Class<?>>(CZAS_ZAKUPOW, Integer.class),
                    new Pair<String, Class<?>>(UPRZYWILEJOWANY, Boolean.class),
                    new Pair<String, Class<?>>(NR_W_KOLEJCE, Integer.class),
                    new Pair<String, Class<?>>(NR_KASY, Integer.class));
            rtiamb.subscribeObjectClassAttributes(klientClassHandle.getClassHandle(),
                    klientClassHandle.createAttributeHandleSet());

            statisticsClassHandle = prepareFomObject(rtiamb.getObjectClassHandle("HLAobjectRoot.Statistics"),
                    new Pair<String, Class<?>>("sredniCzasObslugi", Double.class),
                    new Pair<String, Class<?>>("sredniCzasOczekiwania", Double.class),
                    new Pair<String, Class<?>>("sredniaDlugoscKolejki", Double.class));
            rtiamb.subscribeObjectClassAttributes(statisticsClassHandle.getClassHandle(),
                    statisticsClassHandle.createAttributeHandleSet());

            wejscieDoKolejkiClassHandle = prepareFomInteraction(
                    rtiamb.getInteractionClassHandle("HLAinteractionRoot.wejscieDoKolejki"),
                    new Pair<String, Class<?>>(ID_KLIENT, Integer.class),
                    new Pair<String, Class<?>>(ID_KASA, Integer.class));
            rtiamb.subscribeInteractionClass(wejscieDoKolejkiClassHandle.getClassHandle());

            otworzKaseClassHandle = prepareFomInteraction(
                    rtiamb.getInteractionClassHandle("HLAinteractionRoot.otworzKase"));
            rtiamb.subscribeInteractionClass(otworzKaseClassHandle.getClassHandle());

            closeTheMarketClassHandle = prepareFomInteraction(
                    rtiamb.getInteractionClassHandle("HLAinteractionRoot.zamknijKase"));
            rtiamb.publishInteractionClass(closeTheMarketClassHandle.getClassHandle());

            stopClassHandle = prepareFomInteraction(rtiamb.getInteractionClassHandle("HLAinteractionRoot.stopSymulacji"));
            rtiamb.publishInteractionClass(stopClassHandle.getClassHandle());
        } catch (NameNotFound nameNotFound) {
            nameNotFound.printStackTrace();
        } catch (FederateNotExecutionMember federateNotExecutionMember) {
            federateNotExecutionMember.printStackTrace();
        } catch (RTIinternalError rtIinternalError) {
            rtIinternalError.printStackTrace();
        } catch (SaveInProgress saveInProgress) {
            saveInProgress.printStackTrace();
        } catch (AttributeNotDefined attributeNotDefined) {
            attributeNotDefined.printStackTrace();
        } catch (ConcurrentAccessAttempted concurrentAccessAttempted) {
            concurrentAccessAttempted.printStackTrace();
        } catch (ObjectClassNotDefined objectClassNotDefined) {
            objectClassNotDefined.printStackTrace();
        } catch (RestoreInProgress restoreInProgress) {
            restoreInProgress.printStackTrace();
        } catch (InteractionClassNotDefined interactionClassNotDefined) {
            interactionClassNotDefined.printStackTrace();
        } catch (FederateLoggingServiceCalls federateLoggingServiceCalls) {
            federateLoggingServiceCalls.printStackTrace();
        }


        /*rozpoczecieObslugiClassHandle = prepareFomInteraction(
                rtiamb.getInteractionClassHandle("HLAinteractionRoot.RozpoczecieObslugi"),
                new Pair<String, Class<?>>(CZAS_CZEKANIA_NA_OBSLUGE, Integer.class));
        rtiamb.subscribeInteractionClass(rozpoczecieObslugiClassHandle.getClassHandle());

        koniecObslugiClassHandle = prepareFomInteraction(
                rtiamb.getInteractionClassHandle("HLAinteractionRoot.KoniecObslugi"),
                new Pair<String, Class<?>>(CZAS_OBSLUGI, Integer.class));
        rtiamb.subscribeInteractionClass(koniecObslugiClassHandle.getClassHandle());*/


    }

    public void proceed() {
        this.shouldProceed = true;
    }

    public boolean shouldProceed() {
        return shouldProceed;
    }

    public void scheduleCloseTheMarketInteraction() {
        shouldSendCloseTheMarketInteraction = true;
    }

    public void sendStopInteraction() {
        shouldSendStopInteraction = true;
    }
}
