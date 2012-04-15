import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

@MongoCollection
class Manager extends Employee {
	@MongoField int parkingSpot;
	@MongoField List<Employee> directReports; // must avoid cyclic pickling
	@MongoField Queue<Project> todo;
	@MongoField Map<String, Employee> contacts;

	public Manager() {
		super();
		directReports = new ArrayList<Employee>();
		todo = new LinkedList<Project>();
		contacts = new HashMap<String, Employee>();
	}

	public String toString() {
		String retval = "\n\t{\n\t\t'name':'" + name;
		retval += "',\n\t\t'yearlySalary':'" + yearlySalary;
		if (manager != null)
			retval += "',\n\t\t'manager':'" + manager.name;
		else
			retval += "',\n\t\t'manager':'null";
		retval += "',\n\t\t'projects'" + projects;
		retval += "',\n\t\t'parkingSpot'" + parkingSpot;
		retval += "',\n\t\t'directReports':[";
		for (Employee e : directReports) {
			retval += " " + e.name;
		}
		retval += "]',\n\t\t'todo':" + todo;
		retval += "',\n\t\t'books':" + Arrays.toString(books.toArray());
		retval += "',\n\t\t'contacts':[";
		for (String key : contacts.keySet()) {
			retval += " " + key + ":" + contacts.get(key).name;
		}
		retval += " ]\n\t}\n";

		return retval;
	}
}