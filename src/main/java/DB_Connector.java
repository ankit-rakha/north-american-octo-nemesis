import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.String;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DB_Connector
{
    private static String DB_PATH = "", INPUT_FILE = "", sCurrentLine, node_first_id, node_second_id, nodes_relationship;
    private long time, total, initialTime, compTime, node_init, node_total, rel_init, rel_total;
    private long node_id1, node_id2;
    private Map<String, Object> params = new HashMap<String, Object>();
    private final Map<String, String> config = new HashMap<String, String>();
    private GraphDatabaseService db;
    private ExecutionEngine engine;
    private static final int NTHREDS = 5;

    public static void main( String[] args )
    {
        DB_Connector.DB_PATH = args[0].trim();
        DB_Connector.INPUT_FILE = args[1].trim();
        DB_Connector DB_Connector = new DB_Connector();
        DB_Connector.configParameters();
        DB_Connector.createDatabase();
        DB_Connector.generateTriplet(DB_Connector.db, DB_Connector.engine);

        ExecutorService executor = Executors.newFixedThreadPool(NTHREDS);
        for (int i = 0; i < 10; i++) {
            Runnable worker = new MyRunnable(i,DB_Connector.db, DB_Connector.engine, 1, 2, "is_a");
            executor.execute(worker);
        }
        // This will make the executor accept no new threads
        // and finish all existing threads in the queue
        executor.shutdown();
        // Wait until all threads are finish
        while (!executor.isTerminated()) {

        }
        System.out.println("Finished all threads");

        /*MyRunnable myRunnable1 = new MyRunnable(DB_Connector.db, DB_Connector.engine, 1, 2, "is_a");
        Thread t1 = new Thread(myRunnable1);
        t1.start();
        MyRunnable myRunnable2 = new MyRunnable(DB_Connector.db, DB_Connector.engine,3, 4, "is_a");
        Thread t2 = new Thread(myRunnable2);
        t2.start();
        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
    }

    public void configParameters(){
        config.put( "neostore.nodestore.db.mapped_memory", "1G" );
        config.put( "neostore.relationshipstore.db.mapped_memory", "50G" );
        config.put( "neostore.propertystore.db.mapped_memory", "1G" );
        config.put( "neostore.propertystore.db.strings.mapped_memory", "1G" );
        config.put( "neostore.propertystore.db.arrays.mapped_memory", "1G" );
    }

    public void createDatabase(){

        db = new GraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder( DB_PATH )
                .setConfig( config )
                .newGraphDatabase();

        engine = new ExecutionEngine( db );

        System.out.println("Processing nodes ..");
        node_init = System.currentTimeMillis();
        engine.execute( "START n=node(*) RETURN COUNT(n)" );
        node_total = (System.currentTimeMillis() - node_init);
        System.out.println("took " + node_total + "ms");

        System.out.println("Processing relationships ..");
        rel_init = System.currentTimeMillis();
        engine.execute( "START n=relationship(*) RETURN COUNT(n)" );
        rel_total = (System.currentTimeMillis() - rel_init);
        System.out.println("took " + rel_total + "ms");

    }

    public void generateTriplet(GraphDatabaseService db, ExecutionEngine engine)
    {
        BufferedReader br = null;

        initialTime = System.currentTimeMillis();

        try {

            br = new BufferedReader(new FileReader( INPUT_FILE ));

            while ((sCurrentLine = br.readLine()) != null) {

                StringTokenizer st1 = new StringTokenizer(sCurrentLine,"\t");

                while (st1.hasMoreElements()){

                    // -- FORMAT: NODE1_ID,NODE2_ID,RELATION1_2 --

                    node_first_id = st1.nextElement().toString();
                    node_id1 = Long.valueOf(node_first_id).longValue();

                    node_second_id = st1.nextElement().toString();
                    node_id2 = Long.valueOf(node_second_id).longValue();

                    nodes_relationship = st1.nextElement().toString();

                    runQuery(engine,node_id1,node_id2,nodes_relationship);

                }

            }

        } catch (IOException e) {

            e.printStackTrace();

        } finally {

            try {

                if (br != null)br.close();

            } catch (IOException ex) {

                ex.printStackTrace();
            }
        }

        db.shutdown();

        compTime = (System.currentTimeMillis() - initialTime);
        System.out.println("-- TOTAL TIME: " + compTime + "ms");
    }

    void runQuery(ExecutionEngine engine,long node_id1,long node_id2,String nodes_relationship){

        params.put( "id1", node_id1 );
        params.put( "id2", node_id2 );

        time = System.currentTimeMillis();

        if (engine.execute( "START node1=node({id1}),node2=node({id2}) MATCH (node1)-[:"+nodes_relationship+"]->(node2) RETURN node1", params).columnAs("node1").hasNext()){
            total = (System.currentTimeMillis() - time);
            System.out.println("Found");
            System.out.println("took " + total + "ms");
        }
        else {
            total = (System.currentTimeMillis() - time);
            System.out.println("Not Found");
            System.out.println("took " + total + "ms");

        }

    }

}