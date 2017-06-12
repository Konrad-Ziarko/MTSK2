package fed;

import amb.Ambasador;
import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by konrad on 5/28/17.
 */
public class FederatMenedzer extends AbstractFederat {
    private static final String federateName = "FederatMenedzer";
    private Map<Integer, Integer> queuesSizes = new HashMap<>();
    private static final double maxAverageQueueLength = 6;
    private static final double minAverageQueueLength = 2;

    private boolean checkQueues = false;

    public int getSum() {
        return sum;
    }

    public void setSum(int sum) {
        this.sum = sum;
    }

    private int sum;


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
                if (checkQueues) {
                    setSum(0);
                    queuesSizes.forEach((integer, integer2) -> {
                        setSum(getSum()+integer2);
                    });
                    log("Average queue length = " + getSum()/queuesSizes.size());
                    checkQueues = false;
                }
            }
            advanceTime(timeStep);

        }
    }

    private void prepareFederationAmbassador() {
        this.fedamb = new Ambasador();
        fedamb.registerObjectInstanceCreatedListener((objectHandle, classHandle, objectName) -> {
            objectToClassHandleMap.put(objectHandle, classHandle);
            queuesSizes.put(objectHandle, 0);
            checkQueues = true;
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
                checkQueues = true;
            }
        });

        fedamb.registerInteractionReceivedListener((int interactionClass, ReceivedInteraction theInteraction, byte[] tag, LogicalTime theTime, EventRetractionHandle eventRetractionHandle) -> {
            if (interactionClass == fedamb.startSymulacjiClassHandle.getClassHandle()) {
                log("Start interaction received");
                fedamb.setSimulationStarted(true);
            } else if (interactionClass == fedamb.stopSymulacjiClassHandle.getClassHandle()) {
                log("Stop interaction received");
                fedamb.running = false;
            }
        });
    }

    protected void publishAndSubscribe() {
        try {
            publishZamknijKase();
            publishOtworzKase();

            subscribeWejscieDoKasy();
            subscribeObsluzonoKlienta();

            subscribeNowyKlient();
            subscribeOtworzKase();
            subscribeZamknijKase();
            subscribeOpuszczenieKolejki();
            subscribeWejscieDoKolejki();

            subscribeSimStop();
            subscribeSimStart();
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
