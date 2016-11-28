package de.tu_berlin.snet.cellservice;

import de.tu_berlin.snet.cellservice.model.record.Call;
import de.tu_berlin.snet.cellservice.model.record.Data;
import de.tu_berlin.snet.cellservice.model.record.Handover;
import de.tu_berlin.snet.cellservice.model.record.LocationUpdate;
import de.tu_berlin.snet.cellservice.model.record.TextMessage;

/**
 *
 *
 * @author Markus Paeschke (markus.paeschke@gmail.com)
 * @author dima (arbuzinside@gmail.com)
 *
 */

public interface CDRListener {


    void onHandoverDuringTheCall(Handover handover);

    void onDataSession(Data data);

    void onIncomingCallStart(Call call);

    void onIncomingCallEnd(Call call);

    void onOutgoingCallStart(Call call);

    void onOutgoingCallEnd(Call call);

    void onIncomingTextMessage(TextMessage textMessage);

    void onOutgoingTextMessage(TextMessage textMessage);

    void onLocationUpdate(LocationUpdate locationUpdate);
}
