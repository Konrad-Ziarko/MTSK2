package amb;

import hla.rti.jlc.NullFederateAmbassador;
import org.apache.log4j.spi.LoggerFactory;

import java.util.logging.Logger;

/**
 * Created by konrad on 5/28/17.
 */
public abstract class Ambasador extends NullFederateAmbassador {
    private static final Logger logger = LoggerFactory.getLogger(AbstractFederateAmbassador.class);
    public static final String FEDERATION_NAME = "MarketFederation";
    public static final String READY_TO_RUN = "ReadyToRun";
    public double federateTime = 0.0;
    public double grantedTime = 0.0;
    public double federateLookahead = 1.0;

    public boolean isRegulating = false;
    public boolean isConstrained = false;
    public boolean isAdvancing = false;

    public boolean isAnnounced = false;
    public boolean isReadyToRun = false;

    public boolean running = true;


}
