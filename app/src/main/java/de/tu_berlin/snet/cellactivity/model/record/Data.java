package de.tu_berlin.snet.cellactivity.model.record;

import de.tu_berlin.snet.cellactivity.util.validation.Check;

/**
 * Created by giraffe on 4/17/16.
 */
public class Data {
    private long rxBytes, txBytes;
    private long sessionStart, sessionEnd;
    private CellInfo cell;

    public Data(CellInfo cell, long rxBytes, long txBytes) {
        this(cell, rxBytes, txBytes, System.currentTimeMillis()/1000, System.currentTimeMillis()/1000);
    }

    public Data(CellInfo cell, long rxBytes, long txBytes, long sessionStart, long sessionEnd) {
        setRxBytes(rxBytes);
        setTxBytes(txBytes);
        setSessionStart(sessionStart);
        setSessionEnd(sessionEnd);
        setCell(cell);
    }

    public long getRxBytes() {
        return rxBytes;
    }

    public void setRxBytes(long rxBytes) {
        if(rxBytes < 0) {
            throw new IllegalArgumentException("rxBytes cannot be less than 0");
        }
        this.rxBytes = rxBytes;
    }

    public long getTxBytes() {
        return txBytes;
    }

    public void setTxBytes(long txBytes) {
        if(txBytes < 0) {
            throw new IllegalArgumentException("txBytes cannot be less than 0");
        }
        this.txBytes = txBytes;
    }

    public long getSessionStart() {
        return sessionStart;
    }

    public void setSessionStart(long sessionStart) {
        if(Check.Time.isBefore2016(sessionStart)) {
            throw new IllegalArgumentException("sessionStart should be after 2015");
        }
        this.sessionStart = sessionStart;
    }

    public long getSessionEnd() {
        return sessionEnd;
    }

    public void setSessionEnd(long sessionEnd) {
        if(Check.Time.isBefore(sessionEnd, getSessionStart())) {
            throw new IllegalArgumentException("sessionEnd must be after sessionStart");
        }
        this.sessionEnd = sessionEnd;
    }

    public CellInfo getCell() {
        return cell;
    }

    public void setCell(CellInfo cell) {
        this.cell = cell;
    }

    public void addBytes(long rxBytes, long txBytes, long timestamp) {
        setRxBytes(getRxBytes() + rxBytes);
        setTxBytes(getTxBytes() + txBytes);
        setSessionEnd(timestamp);
    }

    @Override
    public String toString() {
        return "Data(down: "+getRxBytes()+" Bytes, up: "+getTxBytes()+" Bytes)";
    }
}
