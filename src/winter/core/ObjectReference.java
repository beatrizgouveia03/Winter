package winter.core;

public class ObjectReference {
    private final int port;
    private final String host;
    private final String objectID;

    public ObjectReference(int port, String host, String objectID){
        this.port = port;
        this.host = host;
        this.objectID = objectID;
    }

    public int getPort(){ return port; }
    public String getHost(){ return host; }
    public String getObjectID(){ return objectID; }

    public String toURL(){ return "http://" + host + ":" + port + "/invoke?oid=" + objectID; }

    @Override
    public String toString(){
        return "ObjectReference{host='" + host + "\', port=" + port + "\' objectID='"+ objectID + "\'}";
    }
}
