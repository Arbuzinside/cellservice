package de.tu_berlin.snet.cellactivity.database;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import java.io.File;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.Future;

import de.tu_berlin.snet.cellactivity.record.Call;
import de.tu_berlin.snet.cellactivity.record.Data;
import de.tu_berlin.snet.cellactivity.record.Handover;
import de.tu_berlin.snet.cellactivity.record.LocationUpdate;
import de.tu_berlin.snet.cellactivity.record.TextMessage;
import de.tu_berlin.snet.cellactivity.util.CellInfo;
import jsqlite.Database;
import jsqlite.Exception;
import jsqlite.Stmt;
import jsqlite.TableResult;

/**
 * Created by Friedhelm Victor on 4/21/16.
 * This class should implement the Interface MobileNetworkDataCapable
 */
public class GeoDatabaseHelper implements MobileNetworkDataCapable {

    private static final String TAG = "GEODBH";
    private static final String TAG_SL = TAG + "_JSQLITE";
    private static String DB_PATH = "/data/data/de.tu_berlin.snet.cellactivity/databases";
    private static String DB_NAME = "spatial.sqlite";
    private Database mDb;
    private static GeoDatabaseHelper sInstance;

    private final String cellExistsQuery =
            "SELECT id FROM Cells" +
            "   WHERE cellid = %1$s" +
            "   AND lac = %2$s" +
            "   AND mnc = %3$s" +
            "   AND mcc = %4$s" +
            "   AND technology = %5$s";

    private final String callExistsQuery =
            "SELECT id FROM Calls" +
            "   WHERE direction = '%1$s'" +
            "   AND address = '%2$s'" +
            "   AND starttime = %3$s" +
            "   AND endtime = %4$s" +
            "   AND startcell = %5$s";

    public static synchronized GeoDatabaseHelper getInstance(Context context) {

        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (sInstance == null) {
            sInstance = new GeoDatabaseHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    private GeoDatabaseHelper(Context context) {
        try {
            //File sdcardDir = ""; // your sdcard path
            File spatialDbFile = new File(DB_PATH, DB_NAME);

            mDb = new jsqlite.Database();
            mDb.open(spatialDbFile.getAbsolutePath(), jsqlite.Constants.SQLITE_OPEN_READWRITE
                    | jsqlite.Constants.SQLITE_OPEN_CREATE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        createTables(); // TODO: ONLY CREATE TABLES IF THE DATABASE DIDN'T EXIST YET
    }


    /*
    TODO: WHY NOT USE mDb.exec? What about the callback feature?
     */
    synchronized void execSQL(String statement) {
        try {
            Stmt stmt = mDb.prepare(statement);
            stmt.step();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    synchronized void execSQL(String statement, String... args) {
        try {
            Stmt stmt = mDb.prepare(String.format(statement, args));
            stmt.step();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean insertRecord(Call call) {
        CellInfo cellInfo = call.getStartCell();
        insertMeasurement(cellInfo);

        String direction = call.getDirection();
        String address = call.getAddress();
        Long starttime = call.getStartTime();
        Long endtime = call.getEndTime();
        int cellRecordId = getCellPrimaryKey(cellInfo);

        String insertCallRecordStatement =
                "INSERT INTO Calls (direction, address, starttime, endtime, startcell)" +
                "   VALUES ('%1$s', '%2$s', %3$s, %4$s, %5$s);";

        execSQL(String.format(insertCallRecordStatement, direction, address, starttime, endtime, cellRecordId));

        int callId = getCallPrimaryKey(call);

        for(Handover handover : call.getHandovers()) {
            insertRecord(handover, callId);
        }
        return false;
    }

    @Override
    public boolean insertRecord(TextMessage textMessage) {
        CellInfo cellInfo = textMessage.getCell();
        insertMeasurement(cellInfo);
        int cellRecordId = getCellPrimaryKey(cellInfo);
        String insertTextMessageStatement =
                "INSERT INTO TextMessages (direction, address, time, cell_id)" +
                "   VALUES ('%s', '%s', %s, %s);";

        // TODO: Divide timestamp by 1000
        execSQL(String.format(insertTextMessageStatement, textMessage.getDirection(), textMessage.getAddress(), textMessage.getTime(), cellRecordId));
        return false;
    }

    @Override
    public boolean insertRecord(Handover handover, int callId) {
        String insertHandoverStatement =
                "INSERT INTO Handovers (startcell, endcell, time)" +
                "   VALUES (%s, %s, %s);";

        insertMeasurement(handover.getStartCell());
        int startCellId = getCellPrimaryKey(handover.getStartCell());
        insertMeasurement(handover.getEndCell());
        int endCellid = getCellPrimaryKey(handover.getEndCell());

        // TODO: Divide timestamp by 1000
        execSQL(String.format(insertHandoverStatement, startCellId, endCellid, handover.getTimestamp()));

        return false;
    }

    @Override
    public boolean insertRecord(LocationUpdate locationUpdate) {
        String insertLocationUpdateStatement =
                "INSERT INTO LocationUpdates (startcell, endcell, time)" +
                        "   VALUES (%s, %s, %s);";

        insertMeasurement(locationUpdate.getStartCell());
        int startCellId = getCellPrimaryKey(locationUpdate.getStartCell());
        insertMeasurement(locationUpdate.getEndCell());
        int endCellid = getCellPrimaryKey(locationUpdate.getEndCell());

        // TODO: Divide timestamp by 1000
        execSQL(String.format(insertLocationUpdateStatement, startCellId, endCellid, locationUpdate.getTimestamp()));
        return false;
    }

    @Override
    public boolean insertRecord(Data data) {
        CellInfo cellInfo = data.getCell();
        insertMeasurement(cellInfo);

        int cellRecordId = getCellPrimaryKey(cellInfo);
        String insertDataRecordStatement =
                "INSERT INTO DataRecords (rxbytes, txbytes, starttime, endtime, cell_id)" +
                "   VALUES (%s, %s, %s, %s, %s);";
        execSQL(String.format(insertDataRecordStatement, data.getRxBytes(), data.getTxBytes(), data.getSessionStart(), data.getSessionEnd(), cellRecordId));
        return false;
    }


    @Override
    public boolean insertMeasurement(CellInfo cellInfo) {
        String insertCellStatement =
                "INSERT INTO Cells (cellid, lac, mnc, mcc, technology)" +
                "   SELECT %1$s, %2$s, %3$s, %4$s, %5$s" +
                "   WHERE NOT EXISTS (" +
                    cellExistsQuery +
                "   );";

        String cid = String.valueOf(cellInfo.getCellId());
        String lac = String.valueOf(cellInfo.getLac());
        String mnc = String.valueOf(cellInfo.getMnc());
        String mcc = String.valueOf(cellInfo.getMcc());
        String technology = String.valueOf(cellInfo.getConnectionType());

        execSQL(insertCellStatement, cid, lac, mnc, mcc, technology);
        final int cellRecordId = getCellPrimaryKey(cellInfo);
        Log.e("DB", "INSERTED CELL " + cellRecordId);

        final String insertMeasurementStatement =
                "INSERT INTO Measurements (cell_id, provider, accuracy, centroid)" +
                "   VALUES (%s, '%s', %s, GeomFromText('POINT(%s %s)', 4326));";

        // TODO: POSSIBLY BIG PROBLEM HERE WITH final
        // Maybe the Location Futures are being frozen when they actually should still be running
        final ArrayList<Future<Location>> locationMeasurements = cellInfo.getLocations();

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (Future<Location> futureLocation : locationMeasurements) {
                    try {
                        Location location = futureLocation.get();
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        float accuracy = location.getAccuracy();
                        String provider = location.getProvider();
                        String statement = String.format(insertMeasurementStatement, cellRecordId, provider, accuracy, longitude, latitude);
                        Log.e(TAG_SL, "Inserting measurement sql: "+statement);
                        execSQL(statement);
                    } catch (java.lang.Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
        return true;
    }

    private int getCellPrimaryKey(CellInfo cellInfo) {
        String cid = String.valueOf(cellInfo.getCellId());
        String lac = String.valueOf(cellInfo.getLac());
        String mnc = String.valueOf(cellInfo.getMnc());
        String mcc = String.valueOf(cellInfo.getMcc());
        String technology = String.valueOf(cellInfo.getConnectionType());
        return getId(String.format(cellExistsQuery, cid, lac, mnc, mcc, technology));
    }

    private int getCallPrimaryKey(Call call) {
        CellInfo cellInfo = call.getStartCell();
        String direction = call.getDirection();
        String address = call.getAddress();
        Long starttime = call.getStartTime();
        Long endtime = call.getEndTime();
        int cellRecordId = getCellPrimaryKey(cellInfo);
        return getId(String.format(callExistsQuery, direction, address, starttime, endtime, cellRecordId));
    }

    private int getId(String queryWithOneIdResult) {
        try {
            TableResult result = mDb.get_table(queryWithOneIdResult);
            Vector<String[]> rows = result.rows;
            return Integer.valueOf(rows.get(0)[0]);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG_SL, "could not find id with query: "+queryWithOneIdResult);
            return -1;
        }
    }

    @Override
    public void createTables() {
        // Careful! This step can take some time!
        // See http://northredoubt.com/n/2012/06/03/recent-spatialite-news-may-2012/
        //execSQL("SELECT InitSpatialMetaData('WGS84_ONLY');"); // 130 rows
        execSQL("SELECT InitSpatialMetaData('NONE');");
        execSQL("SELECT InsertEpsgSrid(4326);");
        execSQL("SELECT InsertEpsgSrid(32632);");
        execSQL("SELECT InsertEpsgSrid(32633);");
        execSQL("SELECT InsertEpsgSrid(25832);");
        execSQL("SELECT InsertEpsgSrid(25833);");

        String createCellsTable =
                "CREATE TABLE IF NOT EXISTS Cells (" +
                        "   id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                        "   cellid INTEGER," +
                        "   lac INTEGER," +
                        "   mnc INTEGER," +
                        "   mcc INTEGER," +
                        "   technology INTEGER" +
                        "   );";

        String createLocationUpdatesTable =
                "CREATE TABLE IF NOT EXISTS LocationUpdates (" +
                        "   id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                        "   startcell REFERENCES Cells(id)," +
                        "   endcell REFERENCES Cells(id)," +
                        "   time INTEGER" +
                        "   );";

        String createHandoversTable =
                "CREATE TABLE IF NOT EXISTS Handovers (" +
                        "	id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                        "	startcell REFERENCES Cells(id)," +
                        "	endcell REFERENCES Cells(id)," +
                        "   time INTEGER" +
                        "	)";

        String createDataEventsTable =
                "CREATE TABLE IF NOT EXISTS DataRecords (" +
                        "   id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                        "   rxbytes INTEGER," +
                        "   txbytes INTEGER," +
                        "   starttime INTEGER," +
                        "   endtime INTEGER," +
                        "   cell_id INTEGER REFERENCES Cells(id)" +
                        "   );";

        String createCallsTable =
                "CREATE TABLE IF NOT EXISTS Calls (" +
                        "	id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                        "	direction TEXT," +
                        "	address TEXT," +
                        "	starttime INTEGER," +
                        "	endtime INTEGER," +
                        "	startcell INTEGER REFERENCES Cells(id)" +
                        "	);";

        String createTextMessagesTable =
                "CREATE TABLE IF NOT EXISTS TextMessages (" +
                        "	id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                        "	direction TEXT," +
                        "	address TEXT," +
                        "	time INTEGER," +
                        "	cell_id INTEGER REFERENCES Cells(id)" +
                        "	);";

        String createMeasurementsTable =
                "CREATE TABLE IF NOT EXISTS Measurements (" +
                        "	id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                        "	cell_id INTEGER REFERENCES Cells(id)," +
                        "	provider TEXT," +
                        "   accuracy REAL" +
                        "	);";

        String addPointGeometryToMeasurementsTable =
                "SELECT AddGeometryColumn('Measurements', 'centroid', 4326, 'POINT', 'XY', 1);";


        execSQL(createCellsTable);
        execSQL(createLocationUpdatesTable);
        execSQL(createHandoversTable);
        execSQL(createDataEventsTable);
        execSQL(createCallsTable);
        execSQL(createTextMessagesTable);
        execSQL(createMeasurementsTable);
        execSQL(addPointGeometryToMeasurementsTable);
    }

    @Override
    public ArrayList<Call> getCallRecords(Date day) {
        return null;
    }

    @Override
    public ArrayList<Call> getCallRecords(Date from, Date to) {
        return null;
    }

    @Override
    public ArrayList<TextMessage> getTextMessageRecords(Date day) {
        return null;
    }

    @Override
    public ArrayList<TextMessage> getTextMessageRecords(Date from, Date to) {
        return null;
    }

    @Override
    public ArrayList<Handover> getHandoverRecords(Date day) {
        return null;
    }

    @Override
    public ArrayList<Handover> getHandoverRecords(Date from, Date to) {
        return null;
    }

    @Override
    public ArrayList<LocationUpdate> getLocationUpdateRecords(Date day) {
        return null;
    }

    @Override
    public ArrayList<LocationUpdate> getLocationUpdateRecords(Date from, Date to) {
        return null;
    }

    @Override
    public ArrayList<Data> getDataRecords(Date day) {
        return null;
    }

    @Override
    public ArrayList<Data> getDataRecords(Date from, Date to) {
        return null;
    }
}
