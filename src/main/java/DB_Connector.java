import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.String;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.*;

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
        //DB_Connector.configParameters();
        DB_Connector.createDatabase();

        ExecutorService executor = Executors.newFixedThreadPool(NTHREDS);

        BufferedReader br = null;

        try {

            br = new BufferedReader(new FileReader( INPUT_FILE ));
            int count=0;
            while ((sCurrentLine = br.readLine()) != null) {

                StringTokenizer st1 = new StringTokenizer(sCurrentLine,"\t");

                while (st1.hasMoreElements()){

                    // -- FORMAT: NODE1_ID,NODE2_ID,RELATION1_2 --

                    node_first_id = st1.nextElement().toString();
                    long node_id1 = Long.valueOf(node_first_id);

                    node_second_id = st1.nextElement().toString();
                    long node_id2 = Long.valueOf(node_second_id);

                    nodes_relationship = st1.nextElement().toString();

                    Runnable worker = new MyRunnable(DB_Connector.db, DB_Connector.engine, node_id1, node_id2, nodes_relationship);
                    executor.execute(worker);
                    count++;
                    if (count % 1000 == 0) System.out.println(count);
                }

            }

            executor.shutdown();
            executor.awaitTermination(60, SECONDS);
            System.out.println("Finished all threads");
            System.out.println("Total time "+MyRunnable.totalTime+" found "+MyRunnable.totalFound+" not found "+MyRunnable.totalNotFound);

        } catch (Exception e) {

            e.printStackTrace();

        } finally {

            try {

                if (br != null)br.close();

            } catch (IOException ex) {

                ex.printStackTrace();
            }
            DB_Connector.shutdown();
        }

    }

    private void shutdown() {
        db.shutdown();
    }

    public void configParameters(){

        config.put( "neostore.nodestore.db.mapped_memory", "1G" );
        config.put( "neostore.relationshipstore.db.mapped_memory", "50G" );
        config.put( "neostore.propertystore.db.mapped_memory", "1G" );
        config.put( "neostore.propertystore.db.strings.mapped_memory", "1G" );
        config.put( "neostore.propertystore.db.arrays.mapped_memory", "1G" );

    }

    public void createDatabase(){

        final boolean exists = new File(DB_PATH).exists();
        db = new GraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder( DB_PATH )
                .setConfig( config )
                .newGraphDatabase();

        engine = new ExecutionEngine( db );

        if (!exists) {
            System.out.println("Creating test db");
            engine.execute("start n=node(0) foreach(x in range(1,100) : create m={id:x},m-[:KNOWS]->n) return count(*)");
        }

        System.out.println("Processing nodes ..");
        node_init = System.currentTimeMillis();
        ExecutionResult result = engine.execute("START n=node(*) RETURN COUNT(n) as cnt");
        node_total = (System.currentTimeMillis() - node_init);
        System.out.println("took " + node_total + "ms nodes "+result.columnAs("cnt").next());

        System.out.println("Processing relationships ..");
        rel_init = System.currentTimeMillis();
        // Exception in thread "main" java.util.NoSuchElementException: next on empty iterator with 1.9.M04
        result = engine.execute( "START n=relationship(*) RETURN COUNT(n) as cnt" );

        rel_total = (System.currentTimeMillis() - rel_init);
        System.out.println("took " + rel_total + "ms rels"+result.columnAs("cnt").next());

    }

}