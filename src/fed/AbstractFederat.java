package fed;

import amb.Ambasador;
import fom.FomInteraction;
import fom.FomObject;
import fom.FomObjectDefinition;
import hla.rti.*;
import hla.rti.jlc.RtiFactoryFactory;
import org.portico.impl.hla13.types.DoubleTime;
import org.portico.impl.hla13.types.DoubleTimeInterval;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.*;
import java.util.function.Consumer;

/**
 * Created by konrad on 5/28/17.
 */

public abstract class AbstractFederat {
    //Common Strings
    protected static final String NR_KASY = "nrKasy";
    protected static final String NR_KLIENTA = "nrKlienta";
    protected static final String NR_SPRAWY = "nrSprawy";
    protected static final String POZYCJA_KOLEJKI = "pozycjaKolejki";
    protected static final String CZY_UPRZYWILEJOWANY = "czyUprzywilejowany";
    protected static final String RODZAJ_ZALATWIANEJ_SPRAWY = "rodzajZalatwianejSprawy";
    protected static final String NR_OBSLUGIWANEGO_KLIENTA = "nrObslugiwanegoKlienta";
    protected static final String DLUGOSC_KOLEJKI = "dlugoscKolejki";

    protected static final String SREDNI_CZAS_OBSLUGI = "sredniCzasObslugi";
    protected static final String SREDNIA_DLUGOSC_KOLEJKI = "sredniaDlugoscKolejki";
    protected static final String SREDNI_CZAS_OCZEKIWANIA = "sredniCzasOczekiwania";

    protected static final String UPRZYWILEJOWANY = "czyUprzywilejowany";
    protected static final String CZAS_OBSLUGI = "czasObslugi";
    protected static final String CZAS_CZEKANIA_NA_OBSLUGE = "czasCzekaniaNaObsluge";

    protected static final String HLA_KLIENT = "HLAobjectRoot.Klient";
    protected static final String HLA_KASA = "HLAobjectRoot.Kasa";
    protected static final String HLA_STATYSTYKA = "HLAobjectRoot.Statystyka";

    protected static final String HLA_OBSLUZONO_KLIENTA = "HLAinteractionRoot.obsluzonoKlienta";
    protected static final String HLA_OPUSZCENIE_KOLEJKI = "HLAinteractionRoot.opuszczenieKolejki";
    protected static final String HLA_WEJSCIE_DO_KASY = "HLAinteractionRoot.wejscieDoKasy";
    protected static final String HLA_WEJSCIE_DO_KOLEJKI = "HLAinteractionRoot.wejscieDoKolejki";
    protected static final String HLA_OTWORZ_KASE = "HLAinteractionRoot.otworzKase";
    protected static final String HLA_ZAMKNIJ_KASE = "HLAinteractionRoot.zamknijKase";

    protected static final String HLA_STOP_SIM = "HLAinteractionRoot.stopSymulacji";
    protected static final String HLA_START_SIM = "HLAinteractionRoot.startSymulacji";
    protected static final String HLA_NOWY_KLIENT = "HLAinteractionRoot.nowyKlient";

    protected int MAX_SERVICE_TIME = 5000;
    protected int MIN_SERVICE_TIME = 2000;

    //
    public static final String FOM_PATH = "src/fed/bank.xml";
    public static final String federationName = Ambasador.FEDERATION_NAME;
    public static final String READY_TO_RUN = Ambasador.READY_TO_RUN;
    //
    public RTIambassador rtiamb;
    public final double timeStep = 10.0;
    public Ambasador fedamb;
    protected Map<Integer, Integer> objectToClassHandleMap = new HashMap<>();
    protected List<Runnable> queuedTasks = new LinkedList<Runnable>();
    protected void submitNewTask(Runnable task) {
        queuedTasks.add(task);
    }

    protected void executeAllQueuedTasks() {
        queuedTasks.forEach(Runnable::run);
        queuedTasks.clear();
    }


    //
    protected abstract void runFederate();

    protected void waitForUser() {
        log(" >>>>>>>>>> Press Enter to Continue <<<<<<<<<<");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try {
            reader.readLine();
        } catch (Exception e) {
            log("Error while waiting for user input: " + e.getMessage());
            e.printStackTrace();
        }
    }

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
            //urle.printStackTrace();
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

    protected double getLbts() {
        return fedamb.getFederateTime() + fedamb.getFederateLookahead();
    }

    protected byte[] generateTag() {
        return ("" + System.currentTimeMillis()).getBytes();
    }

    public FomObject prepareFomObject(int classHandle,
                                      @SuppressWarnings("unchecked") FomObjectDefinition<String, Class<?>>... attributeNamesAndEncodingFunctions) {
        FomObject object = new FomObject(classHandle);
        iterateArrayWhileDoing(attributeNamesAndEncodingFunctions, pair -> {
            try {
                object.addAttributeHandle(pair.getT1(),
                        rtiamb.getAttributeHandle(pair.getT1(), classHandle),
                        pair.getT2());
            } catch (Exception e) {
                log(1+""+e.getMessage());
            }
        });
        return object;
    }

    public FomInteraction prepareFomInteraction(int classHandle, @SuppressWarnings("unchecked") FomObjectDefinition<String, Class<?>>... parameterNamesAndEncodingFunctions) {
        FomInteraction interaction = new FomInteraction(classHandle);
        iterateArrayWhileDoing(parameterNamesAndEncodingFunctions, pair -> {
            try {
                interaction.addAttributeHandle(pair.getT1(), rtiamb.getParameterHandle(pair.getT1(), classHandle), pair.getT2());
            } catch (Exception e) {
                log(2+""+e.getMessage());
            }
        });
        return interaction;
    }

    private <T> void iterateArrayWhileDoing(T[] array, Consumer<? super T> action) {
        Arrays.stream(array).forEach(pair -> {
            action.accept(pair);
        });
    }


    public void publishOpuszczenieKolejki() throws NameNotFound, FederateNotExecutionMember, RTIinternalError, FederateLoggingServiceCalls, ConcurrentAccessAttempted, InteractionClassNotDefined, RestoreInProgress, SaveInProgress {
        fedamb.opuszczenieKolejkiClassHandle = prepareFomInteraction(
                rtiamb.getInteractionClassHandle(HLA_OPUSZCENIE_KOLEJKI),
                new FomObjectDefinition<>(NR_KLIENTA, Integer.class),
                new FomObjectDefinition<>(NR_KASY, Integer.class));
        rtiamb.publishInteractionClass(fedamb.opuszczenieKolejkiClassHandle.getClassHandle());
    }
    public void subscribeOpuszczenieKolejki() throws NameNotFound, FederateNotExecutionMember, RTIinternalError, FederateLoggingServiceCalls, ConcurrentAccessAttempted, InteractionClassNotDefined, RestoreInProgress, SaveInProgress {
        fedamb.opuszczenieKolejkiClassHandle = prepareFomInteraction(
                rtiamb.getInteractionClassHandle(HLA_OPUSZCENIE_KOLEJKI),
                new FomObjectDefinition<>(NR_KLIENTA, Integer.class),
                new FomObjectDefinition<>(NR_KASY, Integer.class));
        rtiamb.subscribeInteractionClass(fedamb.opuszczenieKolejkiClassHandle.getClassHandle());
    }

    public void subscribeStatystyka() throws NameNotFound, FederateNotExecutionMember, RTIinternalError, ObjectClassNotDefined, ConcurrentAccessAttempted, AttributeNotDefined, RestoreInProgress, SaveInProgress {
        fedamb.statisticsClassHandle = prepareFomObject(rtiamb.getObjectClassHandle(HLA_STATYSTYKA),
                new FomObjectDefinition<>(SREDNI_CZAS_OBSLUGI, Double.class),
                new FomObjectDefinition<>(SREDNI_CZAS_OCZEKIWANIA, Double.class),
                new FomObjectDefinition<>(SREDNIA_DLUGOSC_KOLEJKI, Double.class));
        rtiamb.subscribeObjectClassAttributes(fedamb.statisticsClassHandle.getClassHandle(), fedamb.statisticsClassHandle.createAttributeHandleSet());
    }

    public void publishNowyKlient() throws NameNotFound, FederateNotExecutionMember, RTIinternalError, ConcurrentAccessAttempted, InteractionClassNotDefined, RestoreInProgress, SaveInProgress {
        fedamb.nowyKlientClassHandle = prepareFomInteraction(rtiamb.getInteractionClassHandle(HLA_NOWY_KLIENT),
                new FomObjectDefinition<>(UPRZYWILEJOWANY, Boolean.class) );
        rtiamb.publishInteractionClass(fedamb.nowyKlientClassHandle.getClassHandle());
    }
    public void subscribeNowyKlient() throws NameNotFound, FederateNotExecutionMember, RTIinternalError, FederateLoggingServiceCalls, ConcurrentAccessAttempted, InteractionClassNotDefined, RestoreInProgress, SaveInProgress {
        fedamb.nowyKlientClassHandle = prepareFomInteraction(rtiamb.getInteractionClassHandle(HLA_NOWY_KLIENT),
                new FomObjectDefinition<>(UPRZYWILEJOWANY, Boolean.class));
        rtiamb.subscribeInteractionClass(fedamb.nowyKlientClassHandle.getClassHandle());
    }

    public void publishSimStart() throws NameNotFound, FederateNotExecutionMember, RTIinternalError, ConcurrentAccessAttempted, InteractionClassNotDefined, RestoreInProgress, SaveInProgress {
        fedamb.startSymulacjiClassHandle = prepareFomInteraction(rtiamb.getInteractionClassHandle(HLA_START_SIM));
        rtiamb.publishInteractionClass(fedamb.startSymulacjiClassHandle.getClassHandle());
    }
    public void subscribeSimStart() throws RestoreInProgress, ConcurrentAccessAttempted, InteractionClassNotDefined, SaveInProgress, FederateNotExecutionMember, RTIinternalError, FederateLoggingServiceCalls, NameNotFound {
        fedamb.startSymulacjiClassHandle = prepareFomInteraction(rtiamb.getInteractionClassHandle(HLA_START_SIM));
        rtiamb.subscribeInteractionClass(fedamb.startSymulacjiClassHandle.getClassHandle());
    }

    public void publishSimStop() throws NameNotFound, FederateNotExecutionMember, RTIinternalError, ConcurrentAccessAttempted, InteractionClassNotDefined, RestoreInProgress, SaveInProgress {
        fedamb.stopSymulacjiClassHandle = prepareFomInteraction(rtiamb.getInteractionClassHandle(HLA_STOP_SIM));
        rtiamb.publishInteractionClass(fedamb.stopSymulacjiClassHandle.getClassHandle());
    }
    public void subscribeSimStop() throws NameNotFound, FederateNotExecutionMember, RTIinternalError, FederateLoggingServiceCalls, ConcurrentAccessAttempted, InteractionClassNotDefined, RestoreInProgress, SaveInProgress {
        fedamb.stopSymulacjiClassHandle = prepareFomInteraction(rtiamb.getInteractionClassHandle(HLA_STOP_SIM));
        rtiamb.subscribeInteractionClass(fedamb.stopSymulacjiClassHandle.getClassHandle());
    }

    public void publishWejscieDoKolejki() throws NameNotFound, FederateNotExecutionMember, RTIinternalError, FederateLoggingServiceCalls, ConcurrentAccessAttempted, InteractionClassNotDefined, RestoreInProgress, SaveInProgress {
        fedamb.wejscieDoKolejkiClassHandle = prepareFomInteraction(
                rtiamb.getInteractionClassHandle(HLA_WEJSCIE_DO_KOLEJKI),
                new FomObjectDefinition<>(NR_KLIENTA, Integer.class),
                new FomObjectDefinition<>(NR_KASY, Integer.class),
                new FomObjectDefinition<>(NR_SPRAWY, Integer.class),
                new FomObjectDefinition<>(UPRZYWILEJOWANY, Boolean.class));
        rtiamb.publishInteractionClass(fedamb.wejscieDoKolejkiClassHandle.getClassHandle());
    }
    public void subscribeWejscieDoKolejki() throws NameNotFound, FederateNotExecutionMember, RTIinternalError, FederateLoggingServiceCalls, ConcurrentAccessAttempted, InteractionClassNotDefined, RestoreInProgress, SaveInProgress {
        fedamb.wejscieDoKolejkiClassHandle = prepareFomInteraction(
                rtiamb.getInteractionClassHandle(HLA_WEJSCIE_DO_KOLEJKI),
                new FomObjectDefinition<>(NR_KLIENTA, Integer.class),
                new FomObjectDefinition<>(NR_KASY, Integer.class),
                new FomObjectDefinition<>(NR_SPRAWY, Integer.class),
                new FomObjectDefinition<>(UPRZYWILEJOWANY, Boolean.class));
        rtiamb.subscribeInteractionClass(fedamb.wejscieDoKolejkiClassHandle.getClassHandle());
    }

    public void publishWejscieDoKasy() throws NameNotFound, FederateNotExecutionMember, RTIinternalError, ConcurrentAccessAttempted, InteractionClassNotDefined, RestoreInProgress, SaveInProgress {
        fedamb.wejscieDoKasyClassHandle = prepareFomInteraction(
                rtiamb.getInteractionClassHandle(HLA_WEJSCIE_DO_KASY),
                new FomObjectDefinition<>(CZAS_CZEKANIA_NA_OBSLUGE, Float.class),
                new FomObjectDefinition<>(NR_KASY, Float.class),
                new FomObjectDefinition<>(NR_KLIENTA, Integer.class));
        rtiamb.publishInteractionClass(fedamb.wejscieDoKasyClassHandle.getClassHandle());
    }
    public void subscribeWejscieDoKasy() throws NameNotFound, FederateNotExecutionMember, RTIinternalError, ObjectClassNotDefined, ConcurrentAccessAttempted, AttributeNotDefined, RestoreInProgress, SaveInProgress, InteractionClassNotDefined, FederateLoggingServiceCalls {
        fedamb.wejscieDoKasyClassHandle = prepareFomInteraction(
                rtiamb.getInteractionClassHandle(HLA_WEJSCIE_DO_KASY),
                new FomObjectDefinition<>(CZAS_CZEKANIA_NA_OBSLUGE, Float.class),
                new FomObjectDefinition<>(NR_KASY, Float.class),
                new FomObjectDefinition<>(NR_KLIENTA, Integer.class));
        rtiamb.subscribeInteractionClass(fedamb.wejscieDoKasyClassHandle.getClassHandle());
    }

    public void publishObsluzonoKlienta() throws NameNotFound, FederateNotExecutionMember, RTIinternalError, ConcurrentAccessAttempted, InteractionClassNotDefined, RestoreInProgress, SaveInProgress {
        fedamb.obsluzonoKlientaClassHandle = prepareFomInteraction(
                rtiamb.getInteractionClassHandle(HLA_OBSLUZONO_KLIENTA),
                new FomObjectDefinition<>(CZAS_OBSLUGI, Float.class),
                new FomObjectDefinition<>(NR_KLIENTA, Integer.class));

        rtiamb.publishInteractionClass(fedamb.obsluzonoKlientaClassHandle.getClassHandle());
    }
    public void subscribeObsluzonoKlienta() throws NameNotFound, FederateNotExecutionMember, RTIinternalError, ObjectClassNotDefined, ConcurrentAccessAttempted, AttributeNotDefined, RestoreInProgress, SaveInProgress, InteractionClassNotDefined, FederateLoggingServiceCalls {
        fedamb.obsluzonoKlientaClassHandle = prepareFomInteraction(
                rtiamb.getInteractionClassHandle(HLA_OBSLUZONO_KLIENTA),
                new FomObjectDefinition<>(CZAS_OBSLUGI, Float.class),
                new FomObjectDefinition<>(NR_KLIENTA, Integer.class));
        rtiamb.subscribeInteractionClass(fedamb.obsluzonoKlientaClassHandle.getClassHandle());
    }

    public void publishZamknijKase() throws NameNotFound, FederateNotExecutionMember, RTIinternalError, FederateLoggingServiceCalls, ConcurrentAccessAttempted, InteractionClassNotDefined, RestoreInProgress, SaveInProgress {
        fedamb.zamknijKaseClassHandle = prepareFomInteraction(
                rtiamb.getInteractionClassHandle(HLA_ZAMKNIJ_KASE));
        rtiamb.publishInteractionClass(fedamb.zamknijKaseClassHandle.getClassHandle());
    }
    public void subscribeZamknijKase() throws NameNotFound, FederateNotExecutionMember, RTIinternalError, FederateLoggingServiceCalls, ConcurrentAccessAttempted, InteractionClassNotDefined, RestoreInProgress, SaveInProgress {
        fedamb.zamknijKaseClassHandle = prepareFomInteraction(
                rtiamb.getInteractionClassHandle(HLA_ZAMKNIJ_KASE));
        rtiamb.subscribeInteractionClass(fedamb.zamknijKaseClassHandle.getClassHandle());
    }

    public void publishOtworzKase() throws NameNotFound, FederateNotExecutionMember, RTIinternalError, ConcurrentAccessAttempted, InteractionClassNotDefined, RestoreInProgress, SaveInProgress {
        fedamb.potworzKaseClassHandle = prepareFomInteraction(
                rtiamb.getInteractionClassHandle(HLA_OTWORZ_KASE));
        rtiamb.publishInteractionClass(fedamb.potworzKaseClassHandle.getClassHandle());
    }
    public void subscribeOtworzKase() throws NameNotFound, FederateNotExecutionMember, RTIinternalError, FederateLoggingServiceCalls, ConcurrentAccessAttempted, InteractionClassNotDefined, RestoreInProgress, SaveInProgress {
        fedamb.otworzKaseClassHandle = prepareFomInteraction(
                rtiamb.getInteractionClassHandle(HLA_OTWORZ_KASE));
        rtiamb.subscribeInteractionClass(fedamb.otworzKaseClassHandle.getClassHandle());
    }

    public void publishKasa() throws NameNotFound, FederateNotExecutionMember, RTIinternalError, SaveInProgress, OwnershipAcquisitionPending, ConcurrentAccessAttempted, RestoreInProgress, AttributeNotDefined, ObjectClassNotDefined {
        fedamb.kasaClassHandle = prepareFomObject(rtiamb.getObjectClassHandle(HLA_KASA),
                new FomObjectDefinition<>(NR_KASY, Integer.class),
                new FomObjectDefinition<>(DLUGOSC_KOLEJKI, Integer.class));
        rtiamb.publishObjectClass(fedamb.kasaClassHandle.getClassHandle(), fedamb.kasaClassHandle.createAttributeHandleSet());
    }
    public void subscribeKasa() throws NameNotFound, FederateNotExecutionMember, RTIinternalError, ObjectClassNotDefined, ConcurrentAccessAttempted, AttributeNotDefined, RestoreInProgress, SaveInProgress {
        fedamb.kasaClassHandle = prepareFomObject(rtiamb.getObjectClassHandle(HLA_KASA),
                new FomObjectDefinition<>(NR_KASY, Integer.class),
                new FomObjectDefinition<>(DLUGOSC_KOLEJKI, Integer.class));
        rtiamb.subscribeObjectClassAttributes(fedamb.kasaClassHandle.getClassHandle(), fedamb.kasaClassHandle.createAttributeHandleSet());
    }

    public void publishKlient() throws NameNotFound, FederateNotExecutionMember, RTIinternalError, SaveInProgress, OwnershipAcquisitionPending, ConcurrentAccessAttempted, RestoreInProgress, AttributeNotDefined, ObjectClassNotDefined {
        fedamb.klientClassHandle = prepareFomObject(rtiamb.getObjectClassHandle(HLA_KLIENT),
                new FomObjectDefinition<>(NR_KASY, Integer.class),
                new FomObjectDefinition<>(NR_KLIENTA, Integer.class),
                new FomObjectDefinition<>(CZY_UPRZYWILEJOWANY, Boolean.class),
                new FomObjectDefinition<>(POZYCJA_KOLEJKI, Integer.class),
                new FomObjectDefinition<>(RODZAJ_ZALATWIANEJ_SPRAWY, Integer.class));

        rtiamb.publishObjectClass(fedamb.klientClassHandle.getClassHandle(), fedamb.klientClassHandle.createAttributeHandleSet());
    }
    public void subscribeKlient() throws RTIinternalError, NameNotFound, FederateNotExecutionMember, ObjectClassNotDefined, ConcurrentAccessAttempted, AttributeNotDefined, RestoreInProgress, SaveInProgress {
        fedamb.klientClassHandle = prepareFomObject(rtiamb.getObjectClassHandle(HLA_KLIENT),
                new FomObjectDefinition<>(NR_KASY, Integer.class),
                new FomObjectDefinition<>(NR_KLIENTA, Integer.class),
                new FomObjectDefinition<>(CZY_UPRZYWILEJOWANY, Boolean.class),
                new FomObjectDefinition<>(POZYCJA_KOLEJKI, Integer.class),
                new FomObjectDefinition<>(RODZAJ_ZALATWIANEJ_SPRAWY, Integer.class));
        rtiamb.subscribeObjectClassAttributes(fedamb.klientClassHandle.getClassHandle(), fedamb.klientClassHandle.createAttributeHandleSet());
    }
}
