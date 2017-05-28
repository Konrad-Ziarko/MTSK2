import hla.rti.jlc.NullFederateAmbassador;

/**
 * Created by konrad on 5/28/17.
 */
public class KlientA extends NullFederateAmbassador {
    protected double federateTime = 0.0;
    protected double grantedTime = 0.0;
    protected double federateLookahead = 1.0;

    protected boolean isRegulating = false;
    protected boolean isConstrained = false;
    protected boolean isAdvancing = false;

    protected boolean isAnnounced = false;
    protected boolean isReadyToRun = false;

    protected boolean running = true;
    protected int finishHandle = 0;


}
