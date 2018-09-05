package io.dragonsbane.core;

import io.onemfive.core.BaseService;
import io.onemfive.core.Config;
import io.onemfive.core.ServiceStatus;
import io.onemfive.core.did.DIDService;
import io.onemfive.core.notification.NotificationService;
import io.onemfive.core.notification.SubscriptionRequest;
import io.onemfive.core.sensors.SensorRequest;
import io.onemfive.core.sensors.SensorsService;
import io.onemfive.data.*;
import io.onemfive.data.util.DLC;
import io.onemfive.data.util.JSONParser;

import java.util.*;
import java.util.logging.Logger;

/**
 * Health Record Management
 *
 * @author objectorange
 */
public class HealthRecordService extends BaseService {

    public static final String OPERATION_RECEIVE_LOCAL_DID = "RECEIVE_LOCAL_DID";
    public static final String OPERATION_LOAD_HEALTHRECORD = "LOAD_HEALTHRECORD";

    private static final Logger LOG = Logger.getLogger(HealthRecordService.class.getName());

    private Properties properties;
    private DID userDID;

    public HealthRecordService() {
        super();
    }

    @Override
    public void handleDocument(Envelope e) {
        Route r = e.getRoute();
        switch(r.getOperation()) {
            case OPERATION_RECEIVE_LOCAL_DID: {receiveLocalDID(e);break;}
            case OPERATION_LOAD_HEALTHRECORD: {loadHealthRecord(e);break;}
            default: deadLetter(e);
        }
    }

    private void receiveLocalDID(Envelope e) {
        DID receivedDID = (DID)DLC.getData(DID.class,e);
        if(receivedDID != null) {
            userDID = receivedDID;
        }
    }

    private void loadHealthRecord(Envelope e) {

    }

    private void routeIn(Envelope envelope) {
        LOG.info("Route In from Notification Service...");
        DID fromDid = envelope.getDID();
        EventMessage event = (EventMessage)envelope.getMessage();
        Object content = event.getMessage();
        Map<String,Object> json = (Map<String,Object>)JSONParser.parse(content);
        String type = (String)json.get("type");
        if(type == null) {
            LOG.warning("Attribute 'type' not found in EventMessage. Unable to instantiate packet.");
            deadLetter(envelope);
            return;
        }
        Object obj = null;
        try {
            obj = Class.forName(type).newInstance();
        } catch (InstantiationException e) {
            LOG.warning("Unable to instantiate class: "+type);
            deadLetter(envelope);
            return;
        } catch (IllegalAccessException e) {
            LOG.severe(e.getLocalizedMessage());
            deadLetter(envelope);
            return;
        } catch (ClassNotFoundException e) {
            LOG.warning("Class not on classpath: "+type);
            deadLetter(envelope);
            return;
        }

    }

    public void routeOut(Message msg) {
        LOG.info("Routing out comm packet to Sensors Service...");
        String json = JSONParser.toString(msg);
        Envelope e = Envelope.documentFactory();
        e.setSensitivity(Envelope.Sensitivity.HIGH);
        SensorRequest request = new SensorRequest();
        request.from = new DID();
        request.to = new DID();
        request.content = json;
        DLC.addData(SensorRequest.class, request,e);
        DLC.addRoute(SensorsService.class, SensorsService.OPERATION_SEND,e);
        producer.send(e);
        LOG.info("Comm packet sent.");
    }

    @Override
    public boolean start(Properties p) {
        LOG.info("Starting...");
        updateStatus(ServiceStatus.STARTING);

        properties = p;
        try {
            this.properties = Config.loadFromClasspath("dragonsbane-services.config", p, false);
        } catch (Exception e) {
            LOG.warning(e.getLocalizedMessage());
        }

        // Subscribe to all Text notifications
//        Subscription subscription = new Subscription() {
//            @Override
//            public void notifyOfEvent(Envelope envelope) {
//                routeIn(envelope);
//            }
//        };
//        SubscriptionRequest r = new SubscriptionRequest(EventMessage.Type.TEXT, subscription);
//        Envelope e = Envelope.documentFactory();
//        DLC.addData(SubscriptionRequest.class, r, e);
//        DLC.addRoute(NotificationService.class, NotificationService.OPERATION_SUBSCRIBE, e);
//        producer.send(e);

        // Request Local DID
//        Envelope en = Envelope.headersOnlyFactory();
//        DLC.addRoute(HealthRecordService.class, HealthRecordService.OPERATION_RECEIVE_LOCAL_DID, en);
//        DLC.addRoute(DIDService.class, DIDService.OPERATION_GET_LOCAL_DID, en);
//        producer.send(en);

        updateStatus(ServiceStatus.RUNNING);
        LOG.info("Started.");
        return true;
    }

    @Override
    public boolean shutdown() {
        LOG.info("Shutting down...");
        updateStatus(ServiceStatus.SHUTTING_DOWN);

        updateStatus(ServiceStatus.SHUTDOWN);
        LOG.info("Shutdown.");
        return true;
    }

    @Override
    public boolean gracefulShutdown() {
        LOG.info("Gracefully shutting down...");
        updateStatus(ServiceStatus.GRACEFULLY_SHUTTING_DOWN);

        updateStatus(ServiceStatus.GRACEFULLY_SHUTDOWN);
        LOG.info("Graceful Shutdown.");
        return true;
    }
}
