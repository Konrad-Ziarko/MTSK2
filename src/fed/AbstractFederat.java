package fed;

import amb.Ambasador;
import fom.FomInteraction;
import fom.FomObject;
import fom.Pair;
import hla.rti.*;
import hla.rti.jlc.RtiFactoryFactory;
import org.portico.impl.hla13.types.DoubleTime;
import org.portico.impl.hla13.types.DoubleTimeInterval;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Created by konrad on 5/28/17.
 */

public abstract class AbstractFederat {
    //Common Strings
    protected static final String NR_KASY = "nrKasy";
    protected static final String NR_KLIENTA = "nrKlienta";
    protected static final String POZYCJA_KOLEJKI = "pozycjaKolejki";
    protected static final String CZY_UPRZYWILEJOWANY = "czyUprzywilejowany";
    protected static final String RODZAJ_ZALATWIANEJ_SPRAWY = "rodzajZalatwianejSprawy";
    protected static final String NR_OBSLUGIWANEGO_KLIENTA = "nrObslugiwanegoKlienta";
    protected static final String DLUGOSC_KOLEJKI = "dlugoscKolejki";

    protected static final String SREDNI_CZAS_OBSLUGI = "sredniCzasObslugi";
    protected static final String SREDNIA_DLUGOSC_KOLEJKI = "sredniaDlugoscKolejki";
    protected static final String SREDNI_CZAS_OCZEKIWANIA = "sredniCzasOczekiwania";

    protected static final String UPRZYWILEJOWANY = "czyUprzywilejowany";

    protected static final String HLA_KLIENT = "HLAobjectRoot.Klient";
    protected static final String HLA_WEJSCIE_KLIENT = "HLAinteractionRoot.wejscieDoKolejki";
    protected static final String HLA_STOP_SIM = "HLAinteractionRoot.stopSymulacji";
    protected static final String HLA_START_SIM = "HLAinteractionRoot.startSymulacji";

    //
    public static final String FOM_PATH = "src/fed/bank.xml";
    public static final String federationName = Ambasador.FEDERATION_NAME;
    public static final String READY_TO_RUN = Ambasador.READY_TO_RUN;
    //
    public RTIambassador rtiamb;
    public final double timeStep = 10.0;
    public Ambasador fedamb;
    protected Map<Integer, Integer> objectToClassHandleMap;
    //
    protected abstract void runFederate();

    protected abstract void publishAndSubscribe();
    protected abstract void registerObjects();

    protected abstract void deleteObjects();
    //

    protected void log(String message) {
        System.out.println(federationName + " : " + message);
    }

    protected LogicalTimeInterval convertInterval(double time) {
        return new DoubleTimeInterval(time);
    }

    protected LogicalTime convertTime(double time) {
        return new DoubleTime(time);
    }

    protected void enableTimePolicy() {
        LogicalTime currentTime = convertTime(fedamb.federateTime);
        LogicalTimeInterval lookahead = convertInterval(fedamb.federateLookahead);

        try {
            this.rtiamb.enableTimeRegulation(currentTime, lookahead);
            while (!fedamb.isRegulating) {
                rtiamb.tick();
            }
        } catch (TimeRegulationAlreadyEnabled | EnableTimeRegulationPending | TimeAdvanceAlreadyInProgress | InvalidLookahead | InvalidFederationTime | FederateNotExecutionMember | SaveInProgress | RTIinternalError | RestoreInProgress | ConcurrentAccessAttempted timeRegulationAlreadyEnabled) {
            timeRegulationAlreadyEnabled.printStackTrace();
        }

        try {
            this.rtiamb.enableTimeConstrained();
            while (!fedamb.isConstrained) {
                rtiamb.tick();
            }
        } catch (TimeConstrainedAlreadyEnabled | EnableTimeConstrainedPending | FederateNotExecutionMember | TimeAdvanceAlreadyInProgress | RestoreInProgress | SaveInProgress | ConcurrentAccessAttempted | RTIinternalError timeConstrainedAlreadyEnabled) {
            timeConstrainedAlreadyEnabled.printStackTrace();
        }


    }

    protected void advanceTime(double timeToAdvance) {
        fedamb.isAdvancing = true;
        LogicalTime newTime = convertTime(fedamb.federateTime + timeToAdvance);
        try {
            rtiamb.timeAdvanceRequest(newTime);
            while (fedamb.isAdvancing) {
                rtiamb.tick();
            }
        } catch (InvalidFederationTime | FederationTimeAlreadyPassed | EnableTimeRegulationPending | TimeAdvanceAlreadyInProgress | FederateNotExecutionMember | EnableTimeConstrainedPending | RestoreInProgress | SaveInProgress | ConcurrentAccessAttempted | RTIinternalError invalidFederationTime) {
            invalidFederationTime.printStackTrace();
        }
    }

    protected void joinFederation(String federateName) {
        try {
            rtiamb.joinFederationExecution(federateName, federationName, fedamb);
        } catch (FederateAlreadyExecutionMember | FederationExecutionDoesNotExist | SaveInProgress | RTIinternalError | RestoreInProgress | ConcurrentAccessAttempted federateAlreadyExecutionMember) {
            federateAlreadyExecutionMember.printStackTrace();
        }
        log("Joined Federation as " + federateName);
    }

    protected void createFederation() {
        try {
            rtiamb = RtiFactoryFactory.getRtiFactory().createRtiAmbassador();
        } catch (RTIinternalError rtIinternalError) {
            rtIinternalError.printStackTrace();
        }
        try {
            File fom = new File(FOM_PATH);
            rtiamb.createFederationExecution(federationName, fom.toURI().toURL());
            log("Created Federation");
        } catch (FederationExecutionAlreadyExists exists) {
            log("Didn't create federation, it already existed");
        } catch (MalformedURLException urle) {
            log("Exception processing fom: " + urle.getMessage());
            urle.printStackTrace();
        } catch (ConcurrentAccessAttempted | RTIinternalError | CouldNotOpenFED | ErrorReadingFED concurrentAccessAttempted) {
            concurrentAccessAttempted.printStackTrace();
        }
    }

    protected void registerSyncPoint() {
        try {
            rtiamb.registerFederationSynchronizationPoint(READY_TO_RUN, null);
        } catch (FederateNotExecutionMember | SaveInProgress | RTIinternalError | RestoreInProgress | ConcurrentAccessAttempted federateNotExecutionMember) {
            federateNotExecutionMember.printStackTrace();
        }

        while (!fedamb.isAnnounced) {
            try {
                rtiamb.tick();
            } catch (RTIinternalError | ConcurrentAccessAttempted rtIinternalError) {
                rtIinternalError.printStackTrace();
            }
        }
    }

    protected void achieveSyncPoint() {
        try {
            rtiamb.synchronizationPointAchieved(READY_TO_RUN);
        } catch (SynchronizationLabelNotAnnounced | ConcurrentAccessAttempted | RTIinternalError | RestoreInProgress | SaveInProgress | FederateNotExecutionMember synchronizationLabelNotAnnounced) {
            synchronizationLabelNotAnnounced.printStackTrace();
        }
        log("Achieved sync point: " + READY_TO_RUN + ", waiting for federation...");
        while (!fedamb.isReadyToRun) {
            try {
                rtiamb.tick();
            } catch (RTIinternalError | ConcurrentAccessAttempted rtIinternalError) {
                rtIinternalError.printStackTrace();
            }
        }
    }

    protected void attemptToDestroyFederation() throws RTIinternalError, ConcurrentAccessAttempted {
        try {
            rtiamb.destroyFederationExecution(federationName);
            log("Destroyed Federation");
        } catch (FederationExecutionDoesNotExist dne) {
            log("No need to destroy federation, it doesn't exist");
        } catch (FederatesCurrentlyJoined fcj) {
            log("Didn't destroy federation, federates still joined");
        }
    }

    protected void resignFromFederation() throws FederateOwnsAttributes, FederateNotExecutionMember, InvalidResignAction, RTIinternalError, ConcurrentAccessAttempted {
        rtiamb.resignFederationExecution(ResignAction.NO_ACTION);
        log("Resigned from Federation");
    }

    public void cleanUpFederate() throws RTIexception {
        deleteObjects();
        resignFromFederation();
        attemptToDestroyFederation();
    }

    protected void deleteObject(int handle) throws RTIexception {
        rtiamb.deleteObjectInstance(handle, generateTag());
    }

    protected Ambasador getFederateAmbassador() {
        return fedamb;
    }

    protected double getLbts() {
        return getFederateAmbassador().getFederateTime() + getFederateAmbassador().getFederateLookahead();
    }

    protected byte[] generateTag() {
        return ("" + System.currentTimeMillis()).getBytes();
    }

    public FomObject prepareFomObject(int classHandle,
                                      @SuppressWarnings("unchecked") Pair<String, Class<?>>... attributeNamesAndEncodingFunctions) {
        FomObject object = new FomObject(classHandle);
        iterateArrayWhileDoing(attributeNamesAndEncodingFunctions, pair -> {
            try {
                object.addAttributeHandle(pair.getA(),
                        rtiamb.getAttributeHandle(pair.getA(), classHandle),
                        pair.getB());
            } catch (Exception e) {
                log(e.getMessage());
            }
        });
        return object;
    }

    public FomInteraction prepareFomInteraction(int classHandle,
                                                @SuppressWarnings("unchecked") Pair<String, Class<?>>... parameterNamesAndEncodingFunctions) {
        FomInteraction interaction = new FomInteraction(classHandle);
        iterateArrayWhileDoing(parameterNamesAndEncodingFunctions, pair -> {
            try {
                interaction.addAttributeHandle(pair.getA(),
                        rtiamb.getParameterHandle(pair.getA(), classHandle),
                        pair.getB());
            } catch (Exception e) {
                log(e.getMessage());
            }
        });
        return interaction;
    }

    private <T> void iterateArrayWhileDoing(T[] array, Consumer<? super T> action) {
        Arrays.stream(array).forEach(pair -> {
            action.accept(pair);
        });
    }
}
