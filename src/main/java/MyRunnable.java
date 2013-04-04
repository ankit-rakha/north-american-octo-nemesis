import java.lang.String;
import java.util.HashMap;
import java.util.Map;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;

public class MyRunnable implements Runnable {

    private static String nodes_relationship;
    private long time, total;
    private long node_id1, node_id2;
    private Map<String, Object> params = new HashMap<String, Object>();
    private GraphDatabaseService db;
    private ExecutionEngine engine;

    public MyRunnable( GraphDatabaseService db, ExecutionEngine engine, long node_id1, long node_id2, String nodes_relationship ) {

        this.node_id1 = node_id1;
        this.node_id2 = node_id2;
        this.nodes_relationship = nodes_relationship;
        this.db = db;
        this.engine = engine;

    }

    public void run() {

        try {

            params.put( "id1", node_id1 );
            params.put( "id2", node_id2 );

            time = System.currentTimeMillis();

            ExecutionResult result = engine.execute( "START node1=node({id1}),node2=node({id2}) MATCH (node1)-[:"+nodes_relationship+"]->(node2) RETURN node1", params);

            //org.neo4j.helpers.collection.IteratorUtil.count(result);

            //engine.execute( "START node1=node({id1}),node2=node({id2}) MATCH (node1)-[:"+nodes_relationship+"]->(node2) RETURN node1", params);

            System.out.println("took " + total + "ms");

            /*if (engine.execute( "START node1=node({id1}),node2=node({id2}) MATCH (node1)-[:"+nodes_relationship+"]->(node2) RETURN node1", params).columnAs("node1").hasNext()){
                total = (System.currentTimeMillis() - time);
                System.out.println("Found");
                System.out.println("took " + total + "ms");
            }
            else {
                total = (System.currentTimeMillis() - time);
                System.out.println("Not Found");
                System.out.println("took " + total + "ms");

            }*/

            Thread.sleep(500);

        } catch (InterruptedException e) {

            System.out.println("Interrupted.");

        }

        db.shutdown();

    }

}