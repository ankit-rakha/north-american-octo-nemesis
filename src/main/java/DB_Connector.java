import org.neo4j.cypher.javacompat.ExecutionEngine;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.StringTokenizer;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import java.lang.String;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Map;

public class DB_Connector
{
    private static String DB_PATH = "";
    private static String INPUT_FILE = "";
    private long time, total, initialTime, compTime,node_init,node_total,rel_init,rel_total;
    String resultString;
    long node_id1, node_id2;

    public static void main( String[] args )
    {
        DB_Connector.DB_PATH = args[0].trim();
        DB_Connector.INPUT_FILE = args[1].trim();
        DB_Connector DB_Connector = new DB_Connector();
        DB_Connector.run();
    }

    void run()
    {

        //GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase( DB_PATH );

        Map<String, Object> params = new HashMap<String, Object>();

        Map<String, String> config = new HashMap<String, String>();
        config.put( "neostore.nodestore.db.mapped_memory", "1G" );
        config.put( "neostore.relationshipstore.db.mapped_memory", "50G" );
        config.put( "neostore.propertystore.db.mapped_memory", "10G" );
        config.put( "neostore.propertystore.db.strings.mapped_memory", "10G" );
        config.put( "neostore.propertystore.db.arrays.mapped_memory", "10G" );

        GraphDatabaseService db = new GraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder( DB_PATH )
                .setConfig( config )
                .newGraphDatabase();

        ExecutionEngine engine = new ExecutionEngine( db );

        BufferedReader br = null;

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

        initialTime = System.currentTimeMillis();

        try {

            String sCurrentLine;

            br = new BufferedReader(new FileReader( INPUT_FILE ));

            while ((sCurrentLine = br.readLine()) != null) {

                StringTokenizer st1 = new StringTokenizer(sCurrentLine,"\t");
                while (st1.hasMoreElements()){

                    // -- FORMAT: NODE1_ID,RELATION1_2,NODE2_ID --

                    String node_first_id = st1.nextElement().toString();
                    node_id1 = Long.valueOf(node_first_id).longValue();
                    String node_second_id = st1.nextElement().toString();
                    node_id2 = Long.valueOf(node_second_id).longValue();
                    String nodes_relationship = st1.nextElement().toString();

                    time = System.currentTimeMillis();

                    params.put( "node_id1", node_id1 );
                    params.put( "node_id2", node_id2 );

                    resultString = engine.execute( "START node1=node({node_id1}),node2=node({node_id2}) MATCH p=(node1)-[r:"+nodes_relationship+"]->(node2) RETURN r", params ).dumpToString();

                    if (resultString.toLowerCase().contains(nodes_relationship.toLowerCase()))

                    {

                        System.out.println("FOUND -> " + node_first_id + "\t" + node_second_id + "\t" + nodes_relationship);

                        total = (System.currentTimeMillis() - time);

                        System.out.println("took " + total + "ms");
                    }

                    else

                    {

                        System.out.println("NOT FOUND -> " + node_first_id + "\t" + node_second_id + "\t" + nodes_relationship);

                        total = (System.currentTimeMillis() - time);

                        System.out.println("took " + total + "ms");

                    }

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

}