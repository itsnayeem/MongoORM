import java.util.Date;

@MongoCollection
class Project {
    @MongoField String name;
    @MongoField Date begin;
    @MongoField Date end;
    
    public Project() {
    	name = "Anon Project";
    	begin = null;
    	end = null;
    }
    
    public Project(String n, Date b, Date e) {
        name = n;
        begin = b;
        end = e;
    }
    
    public String toString() {
    	String retval = "{'name':'" + name + "'}";
    	return retval; 
    }
}