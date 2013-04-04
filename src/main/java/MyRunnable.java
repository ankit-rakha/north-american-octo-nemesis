import org.neo4j.graphdb.GraphDatabaseService;
import java.lang.String;
import java.util.HashMap;
import java.util.Map;
import org.neo4j.cypher.javacompat.ExecutionEngine;

public class MyRunnable implements Runnable {

    public static volatile int totalTime,totalFound,totalNotFound;
    private String nodes_relationship;
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

            long time = System.currentTimeMillis();

            final String query = "START node1=node({id1}),node2=node({id2}) MATCH (node1)-[:" + nodes_relationship + "]->(node2) RETURN node1";
            if (engine.execute(query, params).iterator().hasNext()){
                totalFound++;
            }
            else {
                totalNotFound++;
            }
            totalTime += (System.currentTimeMillis() - time);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}