import java.util.Date;
import java.util.List;

import com.mongodb.DB;
import com.mongodb.Mongo;

public class Driver {
    public static void main(String[] args) throws Exception {

        Mongo m = new Mongo();
        DB db = m.getDB("orm");
        MongoORM orm = new MongoORM(db);
        db.dropDatabase();

        Manager parrt = new Manager();
        parrt.name = "parrt";
        parrt.yearlySalary = (float)100.5;
        parrt.projects.add(new Project("proj1", new Date(1330407380), new Date(1330407390)));
        parrt.projects.add(new Project("proj2", new Date(1330407380), new Date(1330407390)));
        parrt.projects.add(new Project("proj3", new Date(1330407380), new Date(1330407390)));

        Employee tombu = new Employee();
        tombu.name = "tombu";
        tombu.yearlySalary = (float)300;
        tombu.projects.add(new Project("proj4", new Date(1330407380), new Date(1330407390)));
        tombu.projects.add(new Project("proj5", new Date(1330407380), new Date(1330407390)));
        tombu.manager = parrt;
        
        parrt.directReports.add(tombu);
        parrt.directReports.add(parrt);
        parrt.manager = parrt;

        parrt.todo.addAll(parrt.projects);
        parrt.contacts.put("awesome", tombu);
        
        tombu.books.add("c");
        tombu.books.add("d");
        
        System.out.println("parrt:" + parrt);
        System.out.println("tombu:" + tombu);
        
        orm.save(tombu);
        orm.save(parrt);
        
        List<Manager> mang = orm.loadAll(Manager.class);
        System.out.println("managers: " + mang);
        
        List<Employee> empls = orm.loadAll(Employee.class);
        System.out.println("employees: "+empls);
    }
}
