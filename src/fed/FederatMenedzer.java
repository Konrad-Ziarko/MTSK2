package fed;

import amb.Ambasador;
import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;
import shared.ExternalTask;
import shared.Kasa;
import shared.Klient;

import java.util.*;

/**
 * Created by konrad on 5/28/17.
 */
public class FederatMenedzer extends AbstractFederat {
    private static final String federateName = "FederateMenedzer";
    //private Map<Integer, Integer> queuesSizes = new HashMap<>();
    private static final double maxAverageQueueLength = 4;
    private static final double minAverageQueueLength = 2;

    private boolean checkQueues = false;

    private int sum;
    private int ileKas = 0;
    private int ileKlientow = 0;

    public static void main(String[] args) {
        new FederatMenedzer().runFederate();
    }


    protected void runFederate() {
        createFederation();
        prepareFederationAmbassador();
        joinFederation(federateName);
        registerSyncPoint();
        waitForUser();
        achieveSyncPoint();
        enableTimePolicy();
        publishAndSubscribe();
        registerObjects();

        System.out.println("\nRuszyli");
        while (fedamb.running) {
            if (fedamb.isSimulationStarted()) {
                executeAllQueuedTasks();
                //submitNewTask(() -> {
                if (checkQueues) {
                    double avg = 0.0;
                    try {
                        avg = (double) ((double) (ileKlientow - ileKas) / (double) ileKas);
                    } catch (ArithmeticException e) {

                    }
                    log("ileKlientow=" + ileKlientow);
                    log("ileKas=" + ileKas);

                    if (avg > maxAverageQueueLength || ileKas == 0) {
                        SuppliedParameters parameters;
                        try {
                            parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
                            rtiamb.sendInteraction(fedamb.potworzKaseClassHandle.getClassHandle(), parameters, generateTag(), convertTime(fedamb.getNextTimeStep()));
                            ileKas++;
                            log("Sending open new checkout interaction 1");
                        } catch (RTIexception e) {
                            log("Couldn't send open checkout interaction, because: " + e.getMessage());
                        }
                    } else if (avg < minAverageQueueLength && ileKas > 1) {
                        SuppliedParameters parameters;
                        try {
                            parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
                            rtiamb.sendInteraction(fedamb.zamknijKaseClassHandle.getClassHandle(), parameters, generateTag(), convertTime(fedamb.getNextTimeStep()));
                            ileKas--;
                            log("Sending close checkout interaction");
                        } catch (RTIexception e) {
                            log("Couldn't send close checkout interaction, because: " + e.getMessage());
                        }

                    }
                    log("Average queue length = " + avg);
                    checkQueues = false;
                }
                //});
                executeAllQueuedTasks();
            }
            advanceTime(timeStep);
            executeAllExternalTasks();

        }
    }

    private void prepareFederationAmbassador() {
        this.fedamb = new Ambasador();

        fedamb.registerObjectInstanceCreatedListener((int theObject, int theObjectClass, String objectName) -> {
            if (theObjectClass == fedamb.klientClassHandle.getClassHandle()) {
                ileKlientow++;
            }
        });
        fedamb.registerObjectInstanceRemovedListener((int theObject, byte[] userSuppliedTag, LogicalTime theTime, EventRetractionHandle retractionHandle) -> {
            externalTasks.add(new ExternalTask(() -> {
                ileKlientow--;
            }, convertTime(theTime)));
        });
        /*fedamb.registerObjectInstanceCreatedListener((objectHandle, classHandle, objectName) -> {
            objectToClassHandleMap.put(objectHandle, classHandle);
            queuesSizes.put(objectHandle, 0);
            log("registerObjectInstanceCreated");
        });
        fedamb.registerAttributesUpdatedListener((objectHandle, theAttributes, tag, theTime, retractionHandle) -> {
            if (queuesSizes.containsKey(objectHandle) && fedamb.kasaClassHandle.getClassHandle() == queuesSizes.get(objectHandle)) {
                int queueSize = -1;
                for (int i = 0; i < theAttributes.size(); i++) {
                    try {
                        byte[] value = theAttributes.getValue(i);
                        if (theAttributes.getAttributeHandle(i) == fedamb.kasaClassHandle.getHandleFor(DLUGOSC_KOLEJKI)) {
                            queueSize = EncodingHelpers.decodeInt(value);
                        }
                    } catch (Exception e) {
                        log(e.getMessage());
                    }
                }
                queuesSizes.put(objectHandle, queueSize);
                log("registerAttributesUpdated + " + queueSize);
            }
        });*/

        fedamb.registerInteractionReceivedListener((int interactionClass, ReceivedInteraction theInteraction, byte[] tag, LogicalTime theTime, EventRetractionHandle eventRetractionHandle) -> {
            externalTasks.add(new ExternalTask(() -> {
                if (interactionClass == fedamb.startSymulacjiClassHandle.getClassHandle()) {
                    log("Start interaction received");
                    fedamb.setSimulationStarted(true);
                } else if (interactionClass == fedamb.stopSymulacjiClassHandle.getClassHandle()) {
                    log("Stop interaction received");
                    fedamb.running = false;
                } else if (interactionClass == fedamb.nowyKlientClassHandle.getClassHandle()) {
                    checkQueues = true;
                } else if (interactionClass == fedamb.otworzKaseClassHandle.getClassHandle()) {
                    log("Recived open checkout interaction");
                    ileKas++;
                }
            }, convertTime(theTime)));
        });
    }

    protected void publishAndSubscribe() {
        try {
            subscribeKlient();
            //subscribeKasa();
            subscribeOtworzKase();
            subscribeNowyKlient();

            subscribeWejscieDoKolejki();
            subscribeSimStop();
            subscribeSimStart();

            publishZamknijKase();
            publishOtworzKase();
        } catch
                (NameNotFound | FederateNotExecutionMember | RTIinternalError | InteractionClassNotDefined | SaveInProgress | ConcurrentAccessAttempted | RestoreInProgress | FederateLoggingServiceCalls | ObjectClassNotDefined | AttributeNotDefined
                        nameNotFound) {
            nameNotFound.printStackTrace();
        }
    }

    protected void registerObjects() {

    }

    protected void deleteObjects() {

    }
}
